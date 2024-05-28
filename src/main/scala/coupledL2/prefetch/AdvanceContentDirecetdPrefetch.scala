package coupledL2.prefetch

import utility.{ChiselDB, Constantin, MemReqSource, ParallelPriorityMux, RRArbiterInit, SRAMTemplate}
import org.chipsalliance.cde.config.Parameters
import chisel3._
import chisel3.util._
import chisel3.DontCare.:=
import coupledL2.utils.{ReplacementPolicy, XSPerfAccumulate}
import coupledL2.{HasCoupledL2Parameters, L2TlbReq, L2ToL1TlbIO, TlbCmd}

case class ACDPParameters(
    cmTableEntries: Int = 128,
    cmTagBits: Int = 18,
    prefetchDepthThreshold: Int = 3,
    firstLevelPageNumHighBits: Int = 38,
    firstLevelPageNumLowBits: Int = 30,
    secondLevelPageNumHighBits: Int = 29,
    secondLevelPageNumLowBits: Int = 21,
    roundMax:       Int = 8,
    tlbReplayCnt:   Int = 10,
    tagLength:      Int = 18,
  )
    extends PrefetchParameters {
  override val hasPrefetchBit:  Boolean = true
  override val hasPrefetchSrc:  Boolean = true
  override val inflightEntries: Int = 16
}

trait HasACDPParams extends HasPrefetcherHelper {
    val acdpParams = prefetchOpt.get.asInstanceOf[ACDPParameters]

    val cmTableEntries = acdpParams.cmTableEntries
    val cmIdxBits = log2Up(cmTableEntries)
    val cmTagBits = acdpParams.cmTagBits
    val inflightEntries = acdpParams.inflightEntries

    val firstLevelPageNumHighBits = acdpParams.firstLevelPageNumHighBits
    val firstLevelPageNumLowBits = acdpParams.firstLevelPageNumLowBits
    val secondLevelPageNumHighBits = acdpParams.secondLevelPageNumHighBits
    val secondLevelPageNumLowBits = acdpParams.secondLevelPageNumLowBits
    val tagLength = acdpParams.tagLength

    val roundMax  = acdpParams.roundMax
    val roundBits = log2Up(roundMax)

    val prefetchDepthBits = log2Up(acdpParams.prefetchDepthThreshold)
    val prefetchDepthThreshold = acdpParams.prefetchDepthThreshold
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
  val pfAddr = UInt(fullVAddrBits.W)
  val ptr = UInt(3.W)
}

class TestMissAddressResp(implicit p: Parameters) extends ACDPBundle {
  val hit = Bool()
  val ptr = UInt(3.W)
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
        val w = Flipped(DecoupledIO(UInt(fullVAddrBits.W)))
        val r = Flipped(new TestMissAddressBundle)
    })
    // RCM table is direct mapped, accessed through high 18 bits of address,
    // each entry holding high 18 bits of address.
    def lineAddr(addr: UInt) = addr(fullVAddrBits - 1, offsetBits)
    def hash1(addr:    UInt) = lineAddr(addr)(cmIdxBits - 1, 0)
    def hash2(addr:    UInt) = lineAddr(addr)(2 * cmIdxBits - 1, cmIdxBits)
    def idx(addr:      UInt) = hash1(addr) ^ hash2(addr)
    def tag(addr: UInt) = if(addr.getWidth >= 39) addr(38,21)
                          else addr
    def cmTableEntry() = new Bundle {
        val valid = Bool()
        val addressHighBits = UInt(cmTagBits.W)
    }

    val cmTable = Module(
        new SRAMTemplate(cmTableEntry(), set = cmTableEntries, way = 1, shouldReset = true, singlePort = true)
    )

    val wAddr = io.w.bits
    cmTable.io.w.req.valid := io.w.valid && !io.r.req.valid
    cmTable.io.w.req.bits.setIdx := idx(wAddr)
    cmTable.io.w.req.bits.data(0).valid := true.B
    cmTable.io.w.req.bits.data(0).addressHighBits := tag(wAddr)

    val rAddr = io.r.req.bits.pfAddr
    val rData = Wire(cmTableEntry())
    cmTable.io.r.req.valid := io.r.req.fire
    cmTable.io.r.req.bits.setIdx := idx(rAddr)
    rData := cmTable.io.r.resp.data(0)
    assert(!RegNext(io.w.fire && io.r.req.fire), "single port SRAM should not read and write at the same time")

    io.w.ready := cmTable.io.w.req.ready && !io.r.req.valid
    io.r.req.ready := true.B
    // io.r.resp.valid := RegNext(cmTable.io.r.req.fire)
    io.r.resp.bits.ptr := RegNext(io.r.req.bits.ptr)
    io.r.resp.valid := RegNext(cmTable.io.r.req.fire)
    io.r.resp.bits.hit := rData.valid && rData.addressHighBits === RegNext(tag(rAddr))
}

class PointerDataRecognition(implicit p: Parameters) extends ACDPModule {
  val io = IO(new Bundle {
    val train = Flipped(DecoupledIO(new PrefetchTrain))
    val pointerAddr = Output(UInt(fullVAddrBits.W)) // data of pointer
    val test = new TestMissAddressBundle
    val continuousPf = DecoupledIO(new continuousPrefetch)
    val prefetchDisable = Output(Bool())
  })

  val pointerAddr = RegInit(0.U(fullVAddrBits.W))
  val prefetchDisable = RegInit(true.B)
  val pfdata = io.train.bits.pfdata
  val hit = io.train.bits.hit
  val compareHighBits = blockBytes - firstLevelPageNumHighBits - 1
  val ptr = RegInit(0.U(3.W))

  require(pfdata.getWidth >= blockBytes * 8)
  def splitData(pfdata: UInt): Vec[UInt] = {
    val result = VecInit.tabulate(8) { i => 
      pfdata((i + 1) * blockBytes - 1, i * blockBytes)
    }
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
  
  when((firstNonZeroDataIdx =/= 0.U && io.train.fire) || 
  (firstNonZeroDataIdx === 0.U && io.train.fire && filteredData(0) =/= 0.U)) {
    prefetchDisable := false.B
  }

  val s_idle :: s_compare :: Nil = Enum(2)
  val state = RegInit(s_idle)
  val testPfAdress = filteredData(ptr) 
  val hitPointerAddr = RegInit(0.U(fullVAddrBits.W))

  when(state === s_idle) {
    ptr := 0.U
    state := s_compare
  }
  // on every eligible L2 miss, we test the prefetched data with miss data
  // if prefetched data hits in CM table, this prefetched data could be a pointer data(address) we need to prefetch
  // The current learning phase finishes at the end of a round when:
  // (1) one of the cache miss address hit
  // (2) the number of ptr equals listlength.
  when(state === s_compare) {
    when(io.test.req.fire) {
      val roundFinish = ptr === 7.U
      ptr := Mux(roundFinish, 0.U, ptr + 1.U)
    }

    when(ptr >= 7.U) {
      state := s_idle  
    }    
  } 
  when(io.test.resp.fire && io.test.resp.bits.hit) {
      pointerAddr := filteredData(io.test.resp.bits.ptr)
  }

  io.train.ready := state === s_compare
  io.pointerAddr := pointerAddr
  io.test.req.valid := state === s_compare && io.train.valid
  io.test.req.bits.pfAddr := testPfAdress
  io.test.req.bits.ptr := ptr
  io.test.resp.ready := true.B
  io.prefetchDisable := prefetchDisable

  val prefetchDepthReg = RegInit(0.U(prefetchDepthBits.W))
  val restartBit = RegInit(false.B)
  // will trigger prefetch
  when(io.train.ready) {
    when(io.train.bits.pfDepth =/= 0.U) {
      prefetchDepthReg := RegNext(io.train.bits.pfDepth-1.U)
    }

    when(io.train.bits.pfsource =/= MemReqSource.Prefetch2L2ACDP.id.U) {
      prefetchDepthReg := RegNext(prefetchDepthThreshold.U)
      restartBit := RegNext(true.B)
    }

    when(hit && io.train.bits.restartBit && io.train.bits.pfsource === MemReqSource.Prefetch2L2ACDP.id.U) {
      prefetchDepthReg := RegNext(prefetchDepthThreshold.U)
      restartBit := RegNext(false.B)
    }

    when(hit && io.train.bits.pfDepth === 2.U && io.train.bits.pfsource === MemReqSource.Prefetch2L2ACDP.id.U) {
      prefetchDepthReg := RegNext(io.train.bits.pfDepth-1.U)
      restartBit := RegNext(true.B)
    }
  }
  io.continuousPf.valid := io.train.ready && pointerAddr =/= 0.U
  io.continuousPf.bits.prefetchDepth := prefetchDepthReg
  io.continuousPf.bits.restartBit := restartBit
}

class AcdpReqBundle(implicit p: Parameters) extends ACDPBundle{
  val full_vaddr = UInt(fullVAddrBits.W)
  val needT = Bool()
  val source = UInt(sourceIdBits.W)
  val isACDP = Bool()
  val restartBit = Bool()
  val pfDepth = UInt(2.W)
  
}

class AcdpReqBufferEntry(implicit p: Parameters) extends ACDPBundle {
  val valid = Bool()
  // for tlb req
  val paddrValid = Bool()
  val vaddrNoOffset = UInt((fullVAddrBits-offsetBits).W)
  val paddrNoOffset = UInt(fullVAddrBits.W)
  val replayEn = Bool()
  val replayCnt = UInt(4.W)
  val needT = Bool()
  val source = UInt(sourceIdBits.W)
  val restartBit = Bool()
  val pfDepth = UInt(2.W)

  def reset(x:UInt): Unit = {
    valid := false.B
    paddrValid := false.B
    vaddrNoOffset := 0.U
    paddrNoOffset := 0.U
    replayEn := false.B
    replayCnt := 0.U
    needT := false.B
    source := 0.U
  }

  def fromAcdpReqBundle(req: AcdpReqBundle) = {
    valid := true.B
    paddrValid := false.B
    vaddrNoOffset := get_block_vaddr(req.full_vaddr)
    paddrNoOffset := 0.U
    replayEn := false.B
    replayCnt := 0.U
    needT := req.needT
    source := req.source
    restartBit := req.restartBit
    pfDepth := req.pfDepth
  }

  def toPrefetchReq(): PrefetchReq = {
    val req = Wire(new PrefetchReq)
    req.tag := parseFullAddress(get_pf_paddr())._1
    req.set := parseFullAddress(get_pf_paddr())._2
    req.vaddr.foreach(_ := vaddrNoOffset)
    req.needT := needT
    req.source := source
    req.pfSource := MemReqSource.Prefetch2L2ACDP.id.U
    req.pfDepth := pfDepth
    req.restartBit := restartBit
    req
  }

  def can_send_pf(): Bool = {
    valid && paddrValid
  }

  def get_pf_paddr(): UInt = {
    Cat(paddrNoOffset, 0.U(offsetBits.W))
  }

  def get_tlb_vaddr(): UInt = {
    Cat(vaddrNoOffset, 0.U(offsetBits.W))
  }

  def update_paddr(paddr: UInt) = {
    paddrValid := true.B
    paddrNoOffset := paddr(paddr.getWidth-1, offsetBits)
    replayEn := false.B
    replayCnt := 0.U
  }

  def update_sent(): Unit = {
    valid := false.B
  }

  def update_excp(): Unit = {
    valid := false.B
  }
}

class ACDPPrefetchReqBuffer(implicit p: Parameters) extends ACDPModule {
  val io = IO(new Bundle {
    val in_req = Flipped(ValidIO(new AcdpReqBundle))
    val tlb_req = new L2ToL1TlbIO(nRespDups = 1)
    val out_req = DecoupledIO(new PrefetchReq)
  })

  // val firstTlbReplayCnt = WireInit(Constantin.createRecord("firstTlbReplayCnt", acdpParams.tlbReplayCnt))
  val firstTlbReplayCnt = acdpParams.tlbReplayCnt.U
  val entries = Seq.fill(REQ_FILTER_SIZE)(Reg(new(AcdpReqBufferEntry)))
  def wayMap[T <: Data](f: Int => T) = VecInit((0 until REQ_FILTER_SIZE).map(f))
  def get_flag(vaddr: UInt) = get_block_vaddr(vaddr)

  val tlb_req_arb = Module(new RRArbiterInit(new L2TlbReq, REQ_FILTER_SIZE))
  val pf_req_arb = Module(new RRArbiterInit(new PrefetchReq, REQ_FILTER_SIZE))

  io.tlb_req.req <> tlb_req_arb.io.out
  io.tlb_req.req_kill := false.B
  io.tlb_req.resp.ready := true.B
  io.out_req <> pf_req_arb.io.out

  /* s0: entries look up */
  val prev_in_valid = RegNext(io.in_req.valid, false.B)
  val prev_in_req = RegEnable(io.in_req.bits, io.in_req.valid)
  val prev_in_flag = get_flag(prev_in_req.full_vaddr)
  // s1 entry update
  val alloc = Wire(Vec(REQ_FILTER_SIZE, Bool()))

  val s0_in_req = io.in_req.bits
  val s0_in_flag = get_flag(s0_in_req.full_vaddr)
  val s0_conflict_prev = prev_in_valid && s0_in_flag === prev_in_flag
  val s0_match_oh = VecInit(entries.indices.map(i =>
    entries(i).valid && entries(i).vaddrNoOffset === s0_in_flag &&
    entries(i).needT === s0_in_req.needT && entries(i).source === s0_in_req.source
  )).asUInt
  val s0_match = Cat(s0_match_oh).orR

  val s0_invalid_vec = wayMap(w => !entries(w).valid && !alloc(w))
  val s0_has_invalid_way = s0_invalid_vec.asUInt.orR
  val s0_invalid_oh = ParallelPriorityMux(s0_invalid_vec.zipWithIndex.map(x => x._1 -> UIntToOH(x._2.U(REQ_FILTER_SIZE.W))))

  val s0_req_valid = io.in_req.valid && !s0_conflict_prev && !s0_match && s0_has_invalid_way
  val s0_tlb_fire_oh = VecInit(tlb_req_arb.io.in.map(_.fire)).asUInt
  val s0_pf_fire_oh = VecInit(pf_req_arb.io.in.map(_.fire)).asUInt

  XSPerfAccumulate(cacheParams, "recv_req", io.in_req.valid)
  XSPerfAccumulate(cacheParams, "recv_req_drop_conflict", io.in_req.valid && s0_conflict_prev)
  XSPerfAccumulate(cacheParams, "recv_req_drop_match", io.in_req.valid && !s0_conflict_prev && s0_match)
  XSPerfAccumulate(cacheParams, "recv_req_drop_full", io.in_req.valid && !s0_conflict_prev && !s0_match && !s0_has_invalid_way)

  /* s1 update and replace */
  val s1_valid = RegNext(s0_req_valid, false.B)
  val s1_in_req = RegEnable(s0_in_req, s0_req_valid)
  val s1_invalid_oh = RegEnable(s0_invalid_oh, 0.U, s0_req_valid)
  val s1_pf_fire_oh = RegNext(s0_pf_fire_oh, 0.U)
  val s1_tlb_fire_oh = RegNext(s0_tlb_fire_oh, 0.U)
  val s1_alloc_entry = Wire(new AcdpReqBufferEntry)
  s1_alloc_entry.fromAcdpReqBundle(s1_in_req)

  /* entry update */
  val exp_drop = Wire(Vec(REQ_FILTER_SIZE, Bool()))
  val miss_drop = Wire(Vec(REQ_FILTER_SIZE, Bool()))
  val miss_first_replay = Wire(Vec(REQ_FILTER_SIZE, Bool()))
  val pf_fired = Wire(Vec(REQ_FILTER_SIZE, Bool()))
  val tlb_fired = Wire(Vec(REQ_FILTER_SIZE, Bool()))
  for ((e, i) <- entries.zipWithIndex){
    alloc(i) := s1_valid && s1_invalid_oh(i)
    pf_fired(i) := s0_pf_fire_oh(i)
    exp_drop(i) := s1_tlb_fire_oh(i) && io.tlb_req.resp.valid && !io.tlb_req.resp.bits.miss &&
      ((e.needT && (io.tlb_req.resp.bits.excp.head.pf.st || io.tlb_req.resp.bits.excp.head.af.st)) ||
      (!e.needT && (io.tlb_req.resp.bits.excp.head.pf.ld || io.tlb_req.resp.bits.excp.head.af.ld)))
    val miss = s1_tlb_fire_oh(i) && io.tlb_req.resp.valid && io.tlb_req.resp.bits.miss
    tlb_fired(i) := s1_tlb_fire_oh(i) && io.tlb_req.resp.valid && !io.tlb_req.resp.bits.miss && !exp_drop(i)
    miss_drop(i) := miss && e.replayEn
    miss_first_replay(i) := miss && !e.replayEn
    
    // old data: update replayCnt
    when(e.valid && e.replayCnt.orR) {
      e.replayCnt := e.replayCnt - 1.U
    }
    // recent data: update tlb resp
    when(tlb_fired(i)){
      e.update_paddr(io.tlb_req.resp.bits.paddr.head)
    }.elsewhen(miss_drop(i)) { // miss
      e.reset(i.U)
    }.elsewhen(miss_first_replay(i)){
      e.replayCnt := firstTlbReplayCnt
      e.replayEn := 1.U
    }.elsewhen(exp_drop(i)){
      e.update_excp()
    }
    // issue data: update pf
    when(pf_fired(i)){
      e.update_sent()
    }
    // new data: update data
    when(alloc(i)){
      e := s1_alloc_entry
    }
  }

  /* tlb & pf */
  for((e, i) <- entries.zipWithIndex){
    //tlb_req_arb.io.in(i).valid := e.valid && !s1_tlb_fire_oh(i) && !s2_tlb_fire_oh(i) && !e.paddrValid && !s1_evicted_oh(i)
    tlb_req_arb.io.in(i).valid := e.valid && !e.paddrValid && !s1_tlb_fire_oh(i) && !e.replayCnt.orR
    tlb_req_arb.io.in(i).bits.vaddr := e.get_tlb_vaddr()
    when(e.needT) {
      tlb_req_arb.io.in(i).bits.cmd := TlbCmd.write
    }.otherwise{
      tlb_req_arb.io.in(i).bits.cmd := TlbCmd.read
    }
    tlb_req_arb.io.in(i).bits.size := 3.U
    tlb_req_arb.io.in(i).bits.kill := false.B
    tlb_req_arb.io.in(i).bits.no_translate := false.B

    pf_req_arb.io.in(i).valid := e.can_send_pf()
    pf_req_arb.io.in(i).bits := e.toPrefetchReq()
  }

  // reset meta to avoid muti-hit problem
  for (i <- 0 until REQ_FILTER_SIZE) {
    when(reset.asBool) {
      entries(i).reset(i.U)
    }
  }
  XSPerfAccumulate(cacheParams, "tlb_req", io.tlb_req.req.valid)
  XSPerfAccumulate(cacheParams, "tlb_miss", io.tlb_req.resp.valid && io.tlb_req.resp.bits.miss)
  XSPerfAccumulate(cacheParams, "tlb_excp",
    io.tlb_req.resp.valid && !io.tlb_req.resp.bits.miss && (
      io.tlb_req.resp.bits.excp.head.pf.st || io.tlb_req.resp.bits.excp.head.af.st ||
      io.tlb_req.resp.bits.excp.head.pf.ld || io.tlb_req.resp.bits.excp.head.af.ld
  ))
  XSPerfAccumulate(cacheParams, "entry_alloc", PopCount(alloc))
  XSPerfAccumulate(cacheParams, "entry_miss_first_replay", PopCount(miss_first_replay))
  XSPerfAccumulate(cacheParams, "entry_miss_drop", PopCount(miss_drop))
  XSPerfAccumulate(cacheParams, "entry_excp", PopCount(exp_drop))
  XSPerfAccumulate(cacheParams, "entry_merge", io.in_req.valid && s0_match)
  XSPerfAccumulate(cacheParams, "entry_pf_fire", PopCount(pf_fired))
}
class AdvanceContentDirecetdPrefetch(implicit p: Parameters) extends ACDPModule {
  val io = IO(new Bundle {
    val train = Flipped(DecoupledIO(new PrefetchTrain))
    val req = DecoupledIO(new PrefetchReq)
    val resp = Flipped(DecoupledIO(new PrefetchResp))
    val tlb_req = new L2ToL1TlbIO(nRespDups = 1)
  })

  val rcmTable = Module(new RecentCacheMissTable)
  val pdRecognition = Module(new PointerDataRecognition)

  val s0_fire = pdRecognition.io.train.fire
  val s1_fire = WireInit(false.B)
  val s0_ready, s1_ready = WireInit(false.B)

  /* s0 train */
  val s0_pointerVAddr = pdRecognition.io.pointerAddr
  val prefetchDisable = pdRecognition.io.prefetchDisable
  val continuousPf = pdRecognition.io.continuousPf

  rcmTable.io.r <> pdRecognition.io.test
  rcmTable.io.w.valid := io.train.fire && !io.train.bits.hit
  rcmTable.io.w.bits := io.train.bits.vaddr.getOrElse(0.U)
  pdRecognition.io.continuousPf.ready := true.B
  io.train.ready := true.B
  io.resp.ready := rcmTable.io.w.ready

  pdRecognition.io.train <> io.train
  /* s1 get or send req */
  val s1_req_valid = RegInit(false.B)
  val s1_needT = RegEnable(io.train.bits.needT, s0_fire)
  val s1_source = RegEnable(io.train.bits.source, s0_fire)
  val s1_restartBit = RegEnable(continuousPf.bits.restartBit, continuousPf.fire)
  val s1_pfDepth = RegEnable(continuousPf.bits.prefetchDepth, continuousPf.fire)
  val s1_pointerVaddr = RegEnable(s0_pointerVAddr, s0_fire)

  // pipeline control signal
  when(s0_fire) {
    s1_req_valid := true.B
  }.elsewhen(s1_fire){
    s1_req_valid := false.B
  }

  s0_ready := io.tlb_req.req.ready && s1_ready || !s1_req_valid
  s1_ready := io.req.ready || !io.req.valid 
  s1_fire := s1_ready && s1_req_valid

  // out value
  io.resp.ready := rcmTable.io.w.ready
  io.tlb_req.resp.ready := true.B
  io.train.ready := true.B

  val reqFilter = Module(new ACDPPrefetchReqBuffer)
  when(prefetchDisable) {
    reqFilter.io.in_req.valid := false.B
    reqFilter.io.in_req.bits := DontCare
  }.otherwise{
    reqFilter.io.in_req.valid := s1_req_valid
    reqFilter.io.in_req.bits.full_vaddr := s1_pointerVaddr
    reqFilter.io.in_req.bits.needT := s1_needT
    reqFilter.io.in_req.bits.source := s1_source
    reqFilter.io.in_req.bits.isACDP := true.B
    reqFilter.io.in_req.bits.pfDepth := s1_pfDepth
    reqFilter.io.in_req.bits.restartBit := s1_restartBit
  }  

  io.tlb_req <> reqFilter.io.tlb_req
  io.req <> reqFilter.io.out_req

  XSPerfAccumulate(cacheParams, "acdp_req", io.req.fire)
  XSPerfAccumulate(cacheParams, "acdp_train", io.train.fire)
  XSPerfAccumulate(cacheParams, "acdp_pointer_address_" + pdRecognition.io.pointerAddr.toString, 
                  pdRecognition.io.pointerAddr =/= 0.U)

}