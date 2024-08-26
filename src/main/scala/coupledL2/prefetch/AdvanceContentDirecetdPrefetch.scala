package coupledL2.prefetch

import utility.{ChiselDB, Constantin, MemReqSource, ParallelPriorityMux, RRArbiterInit, SRAMTemplate}
import org.chipsalliance.cde.config.Parameters
import chisel3._
import chisel3.util._
import chisel3.DontCare.:=
import coupledL2.utils.{ReplacementPolicy, XSPerfAccumulate}
import coupledL2.{HasCoupledL2Parameters, L2TlbReq, L2ToL1TlbIO, TlbCmd}
import java.lang.reflect.Parameter

case class ACDPParameters(
    cmTableEntries: Int = 128,
    cmTagBits: Int = 18,
    firstLevelPageNumHighBits: Int = 38,
    firstLevelPageNumLowBits: Int = 30,
    secondLevelPageNumHighBits: Int = 29,
    secondLevelPageNumLowBits: Int = 21,
    tlbReplayCnt:   Int = 10,
    tagLength:      Int = 18,
    paQEntries: Int = 64,
    paQLatency: Int = 32,
    paQMaxLatency: Int = 256,
  )
    extends PrefetchParameters {
  override val hasPrefetchBit:  Boolean = true
  override val hasPrefetchSrc:  Boolean = true
  override val inflightEntries: Int = 16
}

trait HasACDPParams extends HasPrefetcherHelper {
    val acdpParams = prefetchOpt.get.asInstanceOf[ACDPParameters]

    val cmTableEntries = acdpParams.cmTableEntries
    val cmTagBits = acdpParams.cmTagBits
    val inflightEntries = acdpParams.inflightEntries

    val firstLevelPageNumHighBits = acdpParams.firstLevelPageNumHighBits
    val firstLevelPageNumLowBits = acdpParams.firstLevelPageNumLowBits
    val secondLevelPageNumHighBits = acdpParams.secondLevelPageNumHighBits
    val secondLevelPageNumLowBits = acdpParams.secondLevelPageNumLowBits
    val tagLength = acdpParams.tagLength

    val tlbReplayCnt = acdpParams.tlbReplayCnt

    val paQEntries = acdpParams.paQEntries
    val paQLatency = acdpParams.paQEntries
    val paQMaxLatency = acdpParams.paQMaxLatency
}

abstract class ACDPBundle(implicit val p: Parameters) extends Bundle with HasACDPParams
abstract class ACDPModule(implicit val p: Parameters) extends Module with HasACDPParams

class TestMissAddressReq(implicit p: Parameters) extends ACDPBundle {
  /// find whether prefetch address is in cache miss table
  val pfAddr = UInt((fullVAddrBits-offsetBits).W)
}

class TestMissAddressResp(implicit p: Parameters) extends ACDPBundle {
  val hit = Bool()
}

class TestMissAddressBundle(implicit p: Parameters) extends ACDPBundle {
  val req = DecoupledIO(new TestMissAddressReq)
  val resp = Flipped(DecoupledIO(new TestMissAddressResp))
}

class TrainData(implicit p: Parameters) extends ACDPBundle {
  val pfdata = UInt((blockBytes * 8).W)
}

class RecentCacheMissTable(implicit p: Parameters) extends ACDPModule {
    val io = IO(new Bundle {
        val w = Flipped(DecoupledIO(UInt((fullVAddrBits-offsetBits).W)))
        val r = Flipped(new TestMissAddressBundle)
    })
    // RCM table is direct mapped, accessed through high 18 bits of address,
    // each entry holding high 18 bits of address.
    def lineAddr(addr: UInt) = addr(fullVAddrBits-1, offsetBits) // 33bit
    def hash1(addr:    UInt) = lineAddr(addr)(32, 26)
    def hash2(addr:    UInt) = lineAddr(addr)(25, 19)
    def idx(addr:      UInt) = hash1(addr) ^ hash2(addr)
    def tag(addr:     UInt) = if(addr.getWidth >= 39) addr(38, 21) else addr
    def cmTableEntry() = new Bundle {
        val valid = Bool()
        val addressHighBits = UInt(cmTagBits.W)
        // val missTime = UInt(missTimeBits.W)
    }

    val cmTable = Module(
        new SRAMTemplate(cmTableEntry(), set = cmTableEntries, way = 1, shouldReset = true, singlePort = true)
    )

    val wAddr = Cat(io.w.bits, 0.U(offsetBits.W))
    cmTable.io.w.req.valid := io.w.valid && !io.r.req.valid
    cmTable.io.w.req.bits.setIdx := idx(wAddr)
    cmTable.io.w.req.bits.data(0).valid := true.B
    cmTable.io.w.req.bits.data(0).addressHighBits := tag(wAddr)

    val rAddr = Cat(io.r.req.bits.pfAddr, 0.U(offsetBits.W))
    val rData = Wire(cmTableEntry())
    cmTable.io.r.req.valid := io.r.req.fire
    cmTable.io.r.req.bits.setIdx := idx(rAddr)
    rData := cmTable.io.r.resp.data(0)
    assert(!RegNext(io.w.fire && io.r.req.fire), "single port SRAM should not read and write at the same time")

    io.w.ready := cmTable.io.w.req.ready && !io.r.req.valid
    io.r.req.ready := true.B
    // io.r.resp.bits.ptr := RegNext(io.r.req.bits.ptr)
    io.r.resp.valid := RegNext(cmTable.io.r.req.fire)
    io.r.resp.bits.hit := rData.valid && rData.addressHighBits === RegNext(tag(rAddr))

    class AcdpCmEntry extends Bundle {
      val readAddressHighBits = UInt(cmTagBits.W)
    }
    val l2AcdpCmTable = ChiselDB.createTable("l2AcdpCmTable", new AcdpCmEntry, basicDB = true)
    val data = Wire(new AcdpCmEntry)
    data.readAddressHighBits := rData.addressHighBits
    l2AcdpCmTable.log(data = data, en = io.r.resp.valid, site = "CacheMissTable", clock, reset)

    XSPerfAccumulate(cacheParams, "cmTable_write_times", io.w.fire)
    XSPerfAccumulate(cacheParams, "cmTable_resp_hit", io.r.resp.bits.hit)
    XSPerfAccumulate(cacheParams, "cmTable_resp_fire", io.r.resp.fire)
}
class PointerAddrQueue(implicit p: Parameters) extends ACDPModule{
  val io = IO(new Bundle(){
    val in = Flipped(DecoupledIO(UInt(fullVAddrBits.W)))
    val out = DecoupledIO(UInt(fullVAddrBits.W))
  })

  var setPAqEntries = Constantin.createRecord("PointerAddrQueueEntries", paQEntries)
  /* Setting */
  val IdxWidth = log2Up(paQEntries)
  val LatencyWidth = log2Up(paQMaxLatency)
  class Entry extends Bundle{
    val pointeraddr = UInt(fullVAddrBits.W)
    val cnt = UInt(LatencyWidth.W)
  }
  val queue = RegInit(VecInit(Seq.fill(paQEntries)(0.U.asTypeOf(new Entry))))
  val valids = RegInit(VecInit(Seq.fill(paQEntries)(false.B)))
  val head = RegInit(0.U(IdxWidth.W))
  val tail = RegInit(0.U(IdxWidth.W))
  val empty = head === tail && !valids.last
  val full = head === tail && valids.last
  val outValid = !empty && !queue(head).cnt.orR && valids(head)

  /* In & Out */
  var setPAqLatency = Constantin.createRecord("PointerAddrQueueLatency", paQLatency)
  
  when(io.in.valid && !full) {
    // if queue is full, we drop the new request
    queue(tail).pointeraddr := io.in.bits
    queue(tail).cnt := setPAqLatency // paQLatency.U
    valids(tail) := true.B
    tail := tail + 1.U
  }
  when(outValid && io.out.ready) {
    valids(head) := false.B
    head := head + 1.U
  }
  io.in.ready := true.B
  io.out.valid := outValid
  io.out.bits := queue(head).pointeraddr

  /* Update */
  for(i <- 0 until paQEntries){
    when(queue(i).cnt.orR){
      queue(i).cnt := queue(i).cnt - 1.U
    }
  }

  /* Perf */
  XSPerfAccumulate(cacheParams, "paQ:full", full)
  XSPerfAccumulate(cacheParams, "paQ:empty", empty)
  XSPerfAccumulate(cacheParams, "paQ:entryNumber", PopCount(valids.asUInt))
  XSPerfAccumulate(cacheParams, "paQ:inNumber", io.in.valid)
  XSPerfAccumulate(cacheParams, "paQ:outNumber", io.out.valid)

}
class prefetchDataSplit(implicit p: Parameters) extends ACDPModule {
  val io = IO(new Bundle{
    val pfdata = Flipped(DecoupledIO(UInt((blockBytes * 8).W)))
    val filterData = DecoupledIO(UInt(blockBytes.W))
  })

  val compareHighBits = blockBytes - firstLevelPageNumHighBits - 1
  require(io.pfdata.bits.getWidth >= blockBytes * 8)
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
      val sel = (filteredCount > 0.U) && (splited(i)(blockBytes-1, blockBytes-compareHighBits) === zeroTop25) //&& (splited(i)(1,0) === 0.U(2.W))
      Mux(sel, splited(i), 0.U((splited(0).getWidth).W))
    })
    filtered := filteredVec
    filtered
  }

  val s0_pfdata = RegEnable(io.pfdata.bits, io.pfdata.fire)
  val splitedData = splitData(s0_pfdata)
  val filteredData = filterPoniterData(splitedData)
  val firstNonZeroDataIdx = PriorityEncoder(filteredData.map(_ =/= 0.U(blockBytes.W)))
  val validIdx = RegInit(0.U(3.W))

  val s_idle :: s_send :: Nil = Enum(2)
  val state = RegInit(s_idle)
  val filterAddr = RegInit(0.U(blockBytes.W))

  when(state === s_idle) {
    when (io.pfdata.fire) {
      validIdx := firstNonZeroDataIdx
      state := s_send
    }
  }

  when(state === s_send) {
    when(io.filterData.ready) {
      filterAddr := filteredData(validIdx)
      validIdx := validIdx + 1.U
    }

    when(validIdx === 8.U) {
      validIdx := firstNonZeroDataIdx
      state := s_idle
    }

    when(io.pfdata.fire) {
      validIdx := firstNonZeroDataIdx
    }
  }
  io.filterData.bits := filterAddr
  io.filterData.valid := filteredData(validIdx) =/= 0.U(blockBytes.W) && state === s_send
  io.pfdata.ready := true.B
}
class PointerDataRecognition(implicit p: Parameters) extends ACDPModule {
  val io = IO(new Bundle {
    val train = Flipped(DecoupledIO(new TrainData))
    val pointerAddr = Output(UInt(fullVAddrBits.W)) // data of pointer
    val test = new TestMissAddressBundle
    val prefetchDisable = Output(Bool())
    val pointerAddrValid = Output(Bool())
  })

  val pointerAddr = RegInit(0.U(fullVAddrBits.W))
  val prefetchDisable = RegInit(true.B)
  val pfdata = io.train.bits.pfdata

  val pointerAddrValid = RegInit(false.B)

  val pfDataSplit = Module(new prefetchDataSplit)
  val paQueue = Module(new PointerAddrQueue)
  val filter = RegInit(0.U(blockBytes.W))

  pfDataSplit.io.pfdata.valid := io.train.valid
  pfDataSplit.io.pfdata.bits := io.train.bits.pfdata

  pfDataSplit.io.filterData.ready := true.B
  paQueue.io.in.bits := pfDataSplit.io.filterData.bits
  paQueue.io.in.valid := pfDataSplit.io.filterData.valid

  filter := paQueue.io.out.bits

  when(pfDataSplit.io.filterData.valid) {
    prefetchDisable := false.B
  }

  val s_idle :: s_compare :: Nil = Enum(2)
  val state = RegInit(s_idle)
  val testPfAdress = filter(fullVAddrBits-1, offsetBits)

  paQueue.io.out.ready := state === s_idle

  when(state === s_idle) {
    when(paQueue.io.out.valid) {
      state := s_compare
    }
    pointerAddrValid := false.B
  }
  when(state === s_compare) {
    when(io.test.resp.fire) {
      when(io.test.resp.bits.hit){
        pointerAddr := Cat(testPfAdress, 0.U(offsetBits.W))
        pointerAddrValid := true.B
        state := s_idle
      }.elsewhen(!io.test.resp.bits.hit){
        state := s_idle
      }
    }.otherwise{
      state := s_compare
    }
  }

  io.train.ready := true.B
  io.pointerAddr := pointerAddr
  io.test.req.valid := state === s_compare
  io.test.req.bits.pfAddr := testPfAdress
  io.test.resp.ready := true.B
  io.prefetchDisable := prefetchDisable
  io.pointerAddrValid := pointerAddrValid

  class AcdpPREntry extends Bundle {
    val pointerAddr = UInt(fullVAddrBits.W)
  }
  val l2AcdpPRTable = ChiselDB.createTable("l2AcdpPRTable", new AcdpPREntry, basicDB = true)
  for (i <- 0 until REQ_FILTER_SIZE) {
    val data = Wire(new AcdpPREntry)
    data.pointerAddr := pointerAddr
    l2AcdpPRTable.log(data = data, en = io.test.resp.fire && io.test.resp.bits.hit, site = "PointerDataRecognition", clock, reset)
  }
  XSPerfAccumulate(cacheParams, "pointerAddrValid", pointerAddrValid)
}

class AcdpReqBundle(implicit p: Parameters) extends ACDPBundle{
  val pointer_vaddr = UInt(fullVAddrBits.W)
  val needT = Bool()
  val source = UInt(sourceIdBits.W)
  // val isACDP = Bool()
  val pfSource = UInt(PfSource.pfSourceBits.W)
  val hit_prefetched = Bool() 
}

class AcdpReqBufferEntry(implicit p: Parameters) extends ACDPBundle {
  val valid = Bool()
  val hit_prefetched = Bool()
  // for tlb req
  val paddrValid = Bool()
  val vaddrNoOffset = UInt((fullVAddrBits-offsetBits).W)
  val paddrNoOffset = UInt(fullVAddrBits.W)
  val replayEn = Bool()
  val replayCnt = UInt(4.W)
  // for pf req
  val needT = Bool()
  val source = UInt(sourceIdBits.W)
  val pfSource = UInt(PfSource.pfSourceBits.W)

  def reset(x:UInt): Unit = {
    valid := false.B
    paddrValid := false.B
    vaddrNoOffset := 0.U
    paddrNoOffset := 0.U
    replayEn := false.B
    replayCnt := 0.U
    needT := false.B
    source := 0.U
    pfSource := 0.U
    hit_prefetched := false.B
  }

  def fromAcdpReqBundle(req: AcdpReqBundle) = {
    valid := true.B
    paddrValid := false.B
    vaddrNoOffset := get_block_vaddr(req.pointer_vaddr)
    paddrNoOffset := 0.U
    replayEn := false.B
    replayCnt := 0.U
    needT := req.needT
    source := req.source
    pfSource := req.pfSource
    hit_prefetched := req.hit_prefetched
  }
  
  def isEqualAcdpReq(req: AcdpReqBundle) = {
    valid &&
    vaddrNoOffset === get_block_vaddr(req.pointer_vaddr) &&
    needT === req.needT &&
    source === req.source
  }

  def toPrefetchReq(): PrefetchReq = {
    val req = Wire(new PrefetchReq)
    req.tag := parseFullAddress(get_pf_paddr())._1
    req.set := parseFullAddress(get_pf_paddr())._2
    req.vaddr.foreach(_ := vaddrNoOffset)
    req.needT := needT
    req.source := source
    req.pfSource := MemReqSource.Prefetch2L2ACDP.id.U
    when(!hit_prefetched){
      switch(pfSource) {
        is (MemReqSource.Prefetch2L2ACDP.id.U)  { req.pfSource := MemReqSource.Prefetch2L2ACDP_d1.id.U }
        is (MemReqSource.Prefetch2L2ACDP_d1.id.U)  { req.pfSource := MemReqSource.Prefetch2L2ACDP_d2.id.U  }
        is (MemReqSource.Prefetch2L2ACDP_d2.id.U)  { req.pfSource := MemReqSource.Prefetch2L2ACDP_d3.id.U  }
      }
    }
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

  val acdpfirstTlbReplayCnt = Constantin.createRecord("AcdpfirstTlbReplayCnt", acdpParams.tlbReplayCnt)
  // val acdpfirstTlbReplayCnt = tlbReplayCnt.U

  val entries = Seq.fill(REQ_FILTER_SIZE)(Reg(new(AcdpReqBufferEntry)))
  def wayMap[T <: Data](f: Int => T) = VecInit((0 until REQ_FILTER_SIZE).map(f))
  def get_flag(vaddr: UInt) = get_block_vaddr(vaddr)

  val tlb_req_arb = Module(new RRArbiterInit(new L2TlbReq, REQ_FILTER_SIZE))
  val pf_req_arb = Module(new RRArbiterInit(new PrefetchReq, REQ_FILTER_SIZE))

  io.tlb_req.req <> tlb_req_arb.io.out
  io.tlb_req.req_kill := false.B
  io.tlb_req.resp.ready := true.B
  io.out_req <> pf_req_arb.io.out
  // pf_req_arb.io.out.ready := true.B

  /* s0: entries look up */
  val prev_in_valid = RegNext(io.in_req.valid, false.B)
  val prev_in_req = RegEnable(io.in_req.bits, io.in_req.valid)
  val prev_in_flag = get_flag(prev_in_req.pointer_vaddr)
  // s1 entry update
  val alloc = Wire(Vec(REQ_FILTER_SIZE, Bool()))

  val s0_in_req = io.in_req.bits
  val s0_in_flag = get_flag(s0_in_req.pointer_vaddr)
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
    when(tlb_fired(i)){
      e.update_paddr(io.tlb_req.resp.bits.paddr.head)
    }
    when(miss_drop(i)) { // miss
      e.reset(i.U)
    }
    when(miss_first_replay(i)){
      e.replayCnt := acdpfirstTlbReplayCnt
      e.replayEn := true.B
    }
    when(exp_drop(i)){
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

  class AcdpTlbTestEntry extends Bundle {
    val tlbVaddr = UInt((fullVAddrBits+offsetBits).W)
  }
  val l2AcdpTlbTestTable = ChiselDB.createTable("l2AcdpTlbTestTable", new AcdpTlbTestEntry, basicDB = true)
  for ((e, i) <- entries.zipWithIndex) {
    val data = Wire(new AcdpTlbTestEntry)
    data.tlbVaddr := tlb_req_arb.io.in(i).bits.vaddr
    l2AcdpTlbTestTable.log(data = data, en = tlb_req_arb.io.in(i).valid, site = "AcdpTlbTest", clock, reset)
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
  XSPerfAccumulate(cacheParams, "tlb_fired", PopCount(tlb_fired))
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

  val pointerAddrValid = pdRecognition.io.pointerAddrValid

  val pointerVAddr = pdRecognition.io.pointerAddr
  val trainEnable = Mux(io.train.bits.pfsource === MemReqSource.Prefetch2L2ACDP_d3.id.U 
                        && !io.train.bits.hit_L2, false.B, true.B)
  val prefetchDisable = pdRecognition.io.prefetchDisable

  rcmTable.io.r <> pdRecognition.io.test
  rcmTable.io.w.valid := io.train.valid && !io.train.bits.hit && io.train.bits.vaddr.getOrElse(0.U) =/= 0.U((fullVAddrBits-offsetBits).W)
  // NOTE: vaddr from l1 to l2 has no offset bits
  rcmTable.io.w.bits := io.train.bits.vaddr.getOrElse(0.U)

  pdRecognition.io.train.bits.pfdata := io.train.bits.pfdata
  pdRecognition.io.train.valid := io.train.valid && trainEnable

  val needT = RegEnable(io.train.bits.needT, io.train.fire)
  val source = RegEnable(io.train.bits.source, io.train.fire)
  val pfsource = RegEnable(io.train.bits.pfsource, io.train.fire)
  val pointerVaddr = RegEnable(pointerVAddr, pointerAddrValid)
  val hit_L2 = RegEnable(io.train.bits.hit_L2, io.train.fire)

  // out value
  io.resp.ready := true.B
  io.tlb_req.resp.ready := true.B
  io.train.ready := true.B

  val reqFilter = Module(new ACDPPrefetchReqBuffer)
  when(prefetchDisable) {
    reqFilter.io.in_req.valid := false.B
    reqFilter.io.in_req.bits := DontCare
  }.otherwise{
    reqFilter.io.in_req.valid := pointerAddrValid
    reqFilter.io.in_req.bits.pointer_vaddr := pointerVaddr
    reqFilter.io.in_req.bits.needT := needT
    reqFilter.io.in_req.bits.source := source
    reqFilter.io.in_req.bits.pfSource := pfsource
    reqFilter.io.in_req.bits.hit_prefetched := hit_L2
  }  

  io.tlb_req <> reqFilter.io.tlb_req
  io.req <> reqFilter.io.out_req

  XSPerfAccumulate(cacheParams, "acdp_req", io.req.fire)
  XSPerfAccumulate(cacheParams, "acdp_train", io.train.fire)
  XSPerfAccumulate(cacheParams, "acdp_resp", io.resp.fire)
  XSPerfAccumulate(cacheParams, "acdp_drop_for_disable", pdRecognition.io.pointerAddrValid && prefetchDisable)
  XSPerfAccumulate(cacheParams, "acdp_train_stall_for_tlb_not_ready", io.train.valid && !io.tlb_req.req.ready)

}