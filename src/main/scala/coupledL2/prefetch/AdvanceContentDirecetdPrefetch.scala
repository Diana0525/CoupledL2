package coupledL2.prefetch

import utility.{MemReqSource, SRAMTemplate}
import org.chipsalliance.cde.config.Parameters
import chisel3._
import chisel3.util._
import coupledL2.HasCoupledL2Parameters
import coupledL2.utils.XSPerfAccumulate

case class ACDPParameters(
    cmTableEntries: Int = 128,
    cmTagBits: Int = 18,
    prefetchDepthThreshold: Int = 3,
    firstLevelPageNumHighBits: Int = 38,
    firstLevelPageNumLowBits: Int = 30,
    secondLevelPageNumHighBits: Int = 29,
    secondLevelPageNumLowBits: Int = 21,
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
    val secondLevelPageNumLowBits = acdpParams.secondLevelPageNumLowBits

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
  /// find whether miss address is in cache miss table
  val missAddr = UInt(fullAddressBits.W)
  val testAddr = UInt(cmTagBits.W) 
  val ptr = UInt(log2Up(cmTableEntries).W) // index of address high bists in table

}

class TestMissAddressResp(implicit p: Parameters) extends ACDPBundle {
  val testAddr = UInt(cmTagBits.W)
  val ptr = UInt(cmIdxBits.W)
  val hit = Bool()
}

class TestMissAddressBundle(implicit p: Parameters) extends ACDPBundle {
  val req = DecoupledIO(new TestMissAddressReq)
  val resp = Flipped(DecoupledIO(new TestMissAddressResp))
}

class RecentCacheMissTable(implicit p: Parameters) extends ACDPModule {
    val io = IO(new Bundle {
        val w = Flipped(DecoupledIO(UInt(fullAddressBits.W)))
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

    val rAddr = io.r.req.bits.missAddr
    val rData = Wire(cmTableEntry())
    cmTable.io.r.req.valid := io.r.req.fire
    cmTable.io.r.req.bits.setIdx := rAddr
    rData := cmTable.io.r.resp.data(0)

    assert(!RegNext(io.w.fire && io.r.req.fire), "single port SRAM should not read and write at the same time")

    io.w.ready := cmTable.io.w.req.ready && !io.r.req.valid
    io.r.req.ready := true.B
    io.r.resp.valid := RegNext(cmTable.io.r.req.fire)
    io.r.resp.bits.testAddr := RegNext(io.r.req.bits.testAddr)
    io.r.resp.bits.ptr := RegNext(io.r.req.bits.ptr)
    io.r.resp.bits.hit := rData.valid && rData.addressHighBits === RegNext(tag(rAddr))
}

class PointerDataRecognition(implicit p: Parameters) extends ACDPModule {
  val io = IO(new Bundle {
    val train = new PrefetchTrain 
    val pointerAddr = Output(UInt(fullAddressBits.W)) // data of pointer
    val test = new TestMissAddressBundle
  })

  
}
class AdvanceContentDirecetdPrefetch(implicit p: Parameters) extends ACDPModule {
    val io = IO(new Bundle(){
        val train = Flipped(DecoupledIO(new PrefetchTrain))
        val req = DecoupledIO(new PrefetchReq)
        val resp = Flipped(DecoupledIO(new PrefetchResp))
    })

    val cmTable = Module(new RecentCacheMissTable)
    
}