package coupledL2.prefetch

import utility.{MemReqSource, SRAMTemplate}
import org.chipsalliance.cde.config.Parameters
import chisel3._
import chisel3.util._
import coupledL2.HasCoupledL2Parameters
import coupledL2.utils.XSPerfAccumulate
import freechips.rocketchip.amba.ahb.AHBImpMaster
import freechips.rocketchip.rocket.TLB
import freechips.rocketchip.rocket.TLBReq
import freechips.rocketchip.rocket.TLBResp
import freechips.rocketchip.rocket.TLBPTWIO
import freechips.rocketchip.rocket.constants.MemoryOpConstants
import firrtl.ir.Flip


case class ACDPParameters(
    cmTableEntries: Int = 128,
    cmTagBits: Int = 18,
    prefetchDepthThreshold: Int = 3,
    firstLevelPageNumHighBits: Int = 38,
    firstLevelPageNumLowBits: Int = 30,
    secondLevelPageNumHighBits: Int = 29,
    secondLevelPageNumLowBits: Int = 21,
    roundMax:       Int = 8,
    TLBsize:  Int = 64
  )
    extends PrefetchParameters {
  override val hasPrefetchBit:  Boolean = true
  override val hasPrefetchSrc:  Boolean = true
  override val inflightEntries: Int = 16
}

trait HasACDPParams extends HasCoupledL2Parameters {
    val acdpParams = prefetchOpt.get.asInstanceOf[ACDPParameters]

    val cmTableEntries = acdpParams.cmTableEntries
    val cmIdxBits = log2Up(cmTableEntries)
    val cmTagBits = acdpParams.cmTagBits
    val inflightEntries = acdpParams.inflightEntries

    val firstLevelPageNumHighBits = acdpParams.firstLevelPageNumHighBits
    val firstLevelPageNumLowBits = acdpParams.firstLevelPageNumLowBits
    val secondLevelPageNumHighBits = acdpParams.secondLevelPageNumHighBits
    val secondLevelPageNumLowBits = acdpParams.secondLevelPageNumLowBits

    val roundMax  = acdpParams.roundMax
    val roundBits = log2Up(roundMax)

    val prefetchDepthBits = log2Up(acdpParams.prefetchDepthThreshold)
    val prefetchDepthThreshold = acdpParams.prefetchDepthThreshold

    val TLBsize = acdpParams.TLBsize
    val lgTLBsize = log2Ceil(TLBsize)
    def signedExtend(x: UInt, width: Int): UInt = {
        if (x.getWidth >= width) {
        x
        } else {
        Cat(Fill(width - x.getWidth, x.head(1)), x)
        }
    }
}

abstract class ACDPBundle(implicit val p: Parameters) extends Bundle with HasACDPParams
abstract class ACDPModule(implicit val p: Parameters) extends Module with HasACDPParams

class MissAddressTableEntry(implicit p: Parameters) extends BOPBundle {
  val missAddr = UInt(scoreBits.W)

  def apply(score: UInt) = {
    val entry = Wire(this)
    // entry.offset := offset
    entry.missAddr := missAddr
    entry
  }
}

class TestMissAddressReq(implicit p: Parameters) extends ACDPBundle {
  /// find whether prefetch address is in cache miss table
  val pfAddr = UInt(fullAddressBits.W)
  val ptr = UInt(log2Up(cmTableEntries).W) // index of address high bists in table
}

class TestMissAddressResp(implicit p: Parameters) extends ACDPBundle {
  // val testAddr = UInt(cmTagBits.W)
  val ptr = UInt(cmIdxBits.W)
  val hit = Bool()
}

class TestMissAddressBundle(implicit p: Parameters) extends ACDPBundle {
  val req = DecoupledIO(new TestMissAddressReq)
  val resp = Flipped(DecoupledIO(new TestMissAddressResp))
}

class continuousPrefetch(implicit p: Parameters) extends ACDPBundle {
  // val pfData = UInt((blockBytes * 8).W)
  val prefetchDepth = UInt(prefetchDepthBits.W)
  val restartBit = Bool()
  // val isContinuation = Bool() // whether is first prefetch
}

class RecentCacheMissTable(implicit p: Parameters) extends ACDPModule {
    val io = IO(new Bundle {
        val w = Flipped(DecoupledIO(UInt(vaddrBitsOpt.get.W)))
        val r = Flipped(new TestMissAddressBundle)
    })

    // RCM table is direct mapped, accessed through high 18 bits of address,
    // each entry holding high 18 bits of address.

    def tag(addr: UInt) = addr(firstLevelPageNumHighBits,secondLevelPageNumLowBits)
    def cmTableEntry() = new Bundle {
        val valid = Bool()
        val addressHighBits = UInt(cmTagBits.W)
    }

    val cmTable = Module(
        new SRAMTemplate(cmTableEntry(), set = cmTableEntries, way = 1, shouldReset = true, singlePort = true)
    )

    val wAddr = io.w.bits// TODO: how to make sure this is miss address?
    cmTable.io.w.req.valid := io.w.valid && !io.r.req.valid
    cmTable.io.w.req.bits.setIdx := tag(wAddr)
    cmTable.io.w.req.bits.data(0).valid := true.B
    cmTable.io.w.req.bits.data(0).addressHighBits := tag(wAddr)

    val rAddr = io.r.req.bits.pfAddr
    val rData = Wire(cmTableEntry())
    cmTable.io.r.req.valid := io.r.req.fire
    cmTable.io.r.req.bits.setIdx := rAddr
    rData := cmTable.io.r.resp.data(0)// TODO:确认index找不到数据会读出什么

    assert(!RegNext(io.w.fire && io.r.req.fire), "single port SRAM should not read and write at the same time")

    io.w.ready := cmTable.io.w.req.ready && !io.r.req.valid
    io.r.req.ready := true.B
    io.r.resp.valid := RegNext(cmTable.io.r.req.fire)
    // io.r.resp.bits.testAddr := RegNext(io.r.req.bits.testAddr)
    io.r.resp.bits.ptr := RegNext(io.r.req.bits.ptr)
    io.r.resp.bits.hit := rData.valid && rData.addressHighBits === RegNext(tag(rAddr))
}

class PointerDataRecognition(implicit p: Parameters) extends ACDPModule {
  val io = IO(new Bundle {
    val train = DecoupledIO(new PrefetchTrain) 
    val pointerAddr = Output(UInt(fullAddressBits.W)) // data of pointer
    val test = new TestMissAddressBundle
    // val continuousPfBundle = new continuousPrefetchBundle
    val continuousPf = DecoupledIO(new continuousPrefetch)
  //   val continuousPfReq = DecoupledIO(new continuousPrefetchReq) // output
  //   val continuousPfResp = Flipped(DecoupledIO(new continuousPrefetchResp)) // input
  })

  val pointerAddr = RegInit(0.U(fullAddressBits.W))
  val pfdata = io.train.bits.pfdata
  val hit = io.train.bits.hit
  val compareHighBits = blockBytes - firstLevelPageNumHighBits - 1

  def splitData(pfdata: UInt): Vec[UInt] = {
    val result = VecInit(Seq.tabulate(8) { i => 
      val startBits = i * blockBytes
      val endBits = startBits + blockBytes
      pfdata(startBits, endBits)
    })
    result
  }

  def filterPoniterData(splited: Vec[UInt]): Vec[UInt] = {
    val filtered = Wire(Vec(splited.size, UInt(blockBytes.W)))
    val filteredCount = Wire(UInt(log2Ceil(splited.size + 1).W))
    val zeroTop25 = 0.U(compareHighBits.W)
    filteredCount := PopCount(splited.map(num =>(num >> (blockBytes - compareHighBits)) === zeroTop25))

    val filteredVec = VecInit((0 until splited.size).map { i =>
      val sel = (filteredCount > 0.U) && (splited(i)(blockBytes-1, blockBytes-compareHighBits) === zeroTop25)
      Mux(sel, splited(i), 0.U((splited(0).getWidth).W))
    })
    filtered := filteredVec
    filtered
  }
  val splitedData = splitData(pfdata)
  val filteredData = filterPoniterData(splitedData)
  val firstNonZeroDataIdx = PriorityEncoder(filteredData.map(_ =/= 0.U))

  val ptr = UInt(cmIdxBits.W)

  val s_idle :: s_compare :: Nil = Enum(2)
  val state = RegInit(s_idle)

  when(state === s_idle) {
    ptr := 0.U
    state := s_compare
  }
  // on every eligible L2 miss, we test the prefetched data with miss data
  // if prefetched data hits in CM table, this prefetched data could be a pointer data(address) we need to prefetch
  // The current learning phase finishes at the end of a round when:
  // (1) one of the cache miss address hit
  // (2) the number of rounds equals ROUNDMAX.
  when(state === s_compare) {
    when(io.test.req.fire) {
      val roundFinish = ptr === (8 - 1).U
      ptr := Mux(roundFinish, 0.U, ptr + 1.U)
    }
    
    when(ptr >= (8 - 1).U) {
      state := s_idle
    }

    when(io.test.resp.fire && io.test.resp.bits.hit) {
      pointerAddr := RegNext(filteredData(ptr))
    }
  }

  io.train.ready := state === s_compare
  io.pointerAddr := pointerAddr
  io.test.req.valid := state === s_compare && io.train.valid
  io.test.req.bits.pfAddr := filteredData(ptr) 
  io.test.req.bits.ptr := ptr 
  io.test.resp.ready := true.B

  val prefetchDepthReg = RegInit(0.U(prefetchDepthBits.W))
  val restartBit = RegInit(false.B)
  // will trigger prefetch
  when(pointerAddr =/= 0.U && io.train.ready) {
    when(io.train.bits.pfDepth =/= 0.U) {
      prefetchDepthReg := RegNext(io.train.bits.pfDepth-1.U)
    }

    when(io.train.bits.pfsource =/= MemReqSource.Prefetch2L2ACDP.id.U) {
      prefetchDepthReg := RegNext(prefetchDepthThreshold.U)
      restartBit := RegNext(true.B)
    }

    when(io.train.bits.hit && io.train.bits.restartBit && io.train.bits.pfsource === MemReqSource.Prefetch2L2ACDP.id.U) {
      prefetchDepthReg := RegNext(prefetchDepthThreshold.U)
      restartBit := RegNext(false.B)
    }

    when(io.train.bits.hit && io.train.bits.pfDepth === 2.U && io.train.bits.pfsource === MemReqSource.Prefetch2L2ACDP.id.U) {
      prefetchDepthReg := RegNext(io.train.bits.pfDepth-1.U)
      restartBit := RegNext(true.B)
    }
  }
  io.continuousPf.valid := io.train.ready && pointerAddr =/= 0.U
  io.continuousPf.bits.prefetchDepth := prefetchDepthReg
  io.continuousPf.bits.restartBit := restartBit
}

class connectTLB(implicit p: Parameters) extends ACDPModule with MemoryOpConstants{
  val io = IO(new Bundle {
    val pointerAddr = Input(UInt(fullAddressBits.W)) 
    val req = Decoupled(new TLBReq(lgTLBsize))
    val resp = Input(new TLBResp)
    val paddr = Output(UInt(fullAddressBits.W))
  })
  io.req.bits.vaddr := io.pointerAddr
  io.req.bits.passthrough := false.B
  io.req.bits.size := TLBsize.U
  io.req.bits.cmd := M_PFR
  io.req.bits.prv := 0.U
  io.req.bits.v := false.B

  val pAddress = RegInit(0.U(fullAddressBits.W))
  when (!io.resp.miss && io.resp.prefetchable) {
    pAddress := io.resp.paddr
  }
  io.paddr := pAddress
}
class AdvanceContentDirecetdPrefetch(implicit p: Parameters) extends ACDPModule {
  val io = IO(new Bundle {
    val train = Flipped(DecoupledIO(new PrefetchTrain))
    val req = DecoupledIO(new PrefetchReq)
    val resp = Flipped(DecoupledIO(new PrefetchResp))
    val TLBreq = Decoupled(new TLBReq(lgTLBsize))
    val TLBresp = Input(new TLBResp)
  })

  val cmTable = Module(new RecentCacheMissTable)
  val pdRecognition = Module(new PointerDataRecognition)
  val cTLB = Module(new connectTLB)

  val pointerAddr = RegInit(0.U(fullAddressBits.W))
  val continuousPf = Flipped(new continuousPrefetch)
  val req = Reg(new PrefetchReq)
  val req_valid = RegInit(false.B)
  val missAddress = RegInit(0.U(vaddrBitsOpt.get.W))
  val physicalAddress = RegInit(0.U(fullAddressBits.W))

  cTLB.io.req <> io.TLBreq
  cTLB.io.resp <> io.TLBresp
  physicalAddress := cTLB.pAddress

  cmTable.io.r <> pdRecognition.io.test
  cmTable.io.w.valid := io.train.fire && !io.train.bits.hit
  // TODO: how to make sure cm.io.w.bits is miss address?
  // cmTable.io.w.bits := 
  when(io.train.fire && !io.train.bits.hit) {
    missAddress := io.train.bits.addr
  }
  cmTable.io.w.bits := missAddress
  pdRecognition.io.train <> io.train
  pointerAddr := pdRecognition.io.pointerAddr

  when(io.req.fire) {
    req_valid := false.B
  }
  when(pdRecognition.io.train.fire) {
    req.tag := parseFullAddress(physicalAddress)._1// TODO: change to Sv39 address
    req.set := parseFullAddress(physicalAddress)._2
    req.needT := io.train.bits.needT
    req.source := io.train.bits.source
    req.restartBit := pdRecognition.io.continuousPf.bits.restartBit
    req.pfDepth := pdRecognition.io.continuousPf.bits.prefetchDepth
  }
  
  continuousPf := pdRecognition.io.continuousPf.bits
  pdRecognition.io.continuousPf.ready := true.B

  when(pdRecognition.io.continuousPf.fire) {
    req.pfDepth := continuousPf.prefetchDepth // TODO: logic in PrefetchReq
    req.restartBit := continuousPf.restartBit
  }
  
  io.req.bits := req
  io.req.valid := req_valid
  io.req.bits.pfSource := MemReqSource.Prefetch2L2ACDP.id.U
  io.train.ready := io.req.ready || !req_valid
  io.resp.ready := cmTable.io.w.ready

  XSPerfAccumulate(cacheParams, "acdp_req", io.req.fire)
  XSPerfAccumulate(cacheParams, "acdp_train", io.train.fire)
  XSPerfAccumulate(cacheParams, "acdp_pointer_address_" + pdRecognition.io.pointerAddr.toString, 
                  pdRecognition.io.pointerAddr =/= 0.U)

}