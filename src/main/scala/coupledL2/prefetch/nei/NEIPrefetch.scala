package coupledL2.prefetch

import org.chipsalliance.cde.config.Parameters
import chisel3._
import chisel3.util._
import coupledL2.HasCoupledL2Parameters
import utility.{ChiselDB, Constantin, MemReqSource, ParallelPriorityMux, RRArbiterInit, SRAMTemplate, XSPerfAccumulate}
import org.chipsalliance.cde.config.Parameters


class InputCheckUpdateRPIO(implicit p: Parameters) extends NEIBundle {
  // val addr = Input(UInt(log2AddrNum.W))
  val enable = Bool()
  val cacheHit = Bool()
  val pn = UInt(log2PageNum.W)
  val offset = UInt(log2BlockNum.W)
}

class OutputCheckUpdateRPIO(implicit p: Parameters) extends NEIBundle {
  val valid = Bool()
  val pattern = UInt(patternWidth.W)
  val mask = UInt(64.W)
}

class RecentPageTableIO(implicit p: Parameters) extends NEIBundle {
  val req = Input(new InputCheckUpdateRPIO)
  val resp = Output(new OutputCheckUpdateRPIO)
}

class InputPrefIO(implicit p: Parameters) extends NEIBundle {
  val enable = Bool()
  val addr = UInt(log2AddrNum.W)
  val cacheHit = Bool()
  val needT = Bool()
  val source = UInt(sourceIdBits.W)
}

class OutputPrefIO(implicit p: Parameters) extends NEIBundle {
  val valid = Bool()
  // val triggerAddr = UInt(log2AddrNum.W)
  val baseAddr = UInt(log2AddrNum.W)
  // val mask = UInt(64.W)
  val pattern = UInt(patternWidth.W)
  // val src = UInt(20.W)
  val source = UInt(sourceIdBits.W)
  val needT = Bool()
}

class NeiIO(implicit p: Parameters) extends NEIBundle {
  val req = Input(new InputPrefIO)
  val resp = Output(new OutputPrefIO)
}

class Nei(implicit p: Parameters) extends NEIModule {

  val io = IO(new NeiIO)
  val recentPageTable = Module(new RecentPageTable)
  val accessCount = RegInit(0.U(64.W))

  def parseAddr(addr: UInt): (UInt, UInt, UInt) = {
    assert(addr.getWidth == log2AddrNum)
    val pn = addr(log2AddrNum - 1, log2PageSize)
    val set = addr(log2PageSize + log2SetNum - 1, log2PageSize)
    val offset = addr(log2PageSize - 1, log2BlockSize)

    (pn, set, offset)
  }

  def getBaseAddr(addr: UInt): UInt = {
    parseAddr(addr)._1 << log2PageSize
  }

  def timeStamp(timer: UInt): UInt = {
    (timer >> 5.U) & timeStampMask.U
  }

  // default output======================================
  io.resp.valid := false.B
  io.resp.baseAddr := getBaseAddr(io.req.addr)
  io.resp.pattern := 0.U
  io.resp.source := io.req.source
  io.resp.needT := io.req.needT
  // default output======================================

  val enable = io.req.enable
  val addr = io.req.addr
  val cacheHit = io.req.cacheHit
  val globalTimer = timeStamp(accessCount)
  val (pn, set, offset) = parseAddr(addr)
  val masktmp = offsetToPattern(offset)
  // internal wire ==========================================

    // default internal wire==========================================
  recentPageTable.io.req := 0.U.asTypeOf(new InputCheckUpdateRPIO)
  // default internal wire==========================================


  when(enable) {
    accessCount := accessCount + 1.U

    // updateRPTable
    // recentPageTable.io.req.addr := addr
    recentPageTable.io.req.enable := true.B
    recentPageTable.io.req.cacheHit := cacheHit
    recentPageTable.io.req.pn := pn
    recentPageTable.io.req.offset := offset

    when(!cacheHit) {
        io.resp.valid := recentPageTable.io.resp.valid
        io.resp.pattern := recentPageTable.io.resp.pattern
    }
  }

  XSPerfAccumulate("nei_needTotal",   enable && !cacheHit);
  XSPerfAccumulate("nei_selectTotal", io.resp.valid);
}

class NEIPatternBufferQueue(implicit p: Parameters) extends NEIModule {
  val io = IO(new Bundle {
    val enq = Flipped(DecoupledIO(new OutputPrefIO))
    val deq = DecoupledIO(new OutputPrefIO)
  })
  /*  Here we implement a queue that
   *  1. is pipelined  2. flows
   *  3. always has the latest reqs, which means the queue is always ready for enq and deserting the eldest ones
   */
  val queue = RegInit(VecInit(Seq.fill(patternBufferQueueSize)(0.U.asTypeOf(new OutputPrefIO))))
  val valids = RegInit(VecInit(Seq.fill(patternBufferQueueSize)(false.B)))
  val idxWidth = log2Up(patternBufferQueueSize)
  val head = RegInit(0.U(idxWidth.W))
  val tail = RegInit(0.U(idxWidth.W))
  val empty = head === tail && !valids.last
  val full = head === tail && valids.last

  when(!empty && io.deq.ready) {
    valids(head) := false.B
    head := head + 1.U
  }

  when(io.enq.valid) {
    queue(tail) := io.enq.bits
    valids(tail) := !empty || !io.deq.ready
    tail := tail + (!empty || !io.deq.ready).asUInt
    when(full && !io.deq.ready) {
      head := head + 1.U
    }
  }

  io.enq.ready := true.B
  io.deq.valid := !empty || io.enq.valid
  io.deq.bits := Mux(empty, io.enq.bits, queue(head))

  val overflow = io.enq.fire && full && !io.deq.fire

  XSPerfAccumulate("NEIPBQEnqPtn", io.enq.fire)
  XSPerfAccumulate("NEIPBQOflPtn", overflow)
  XSPerfAccumulate("NEIPBQDeqPtn", io.deq.fire)

  XSPerfAccumulate("NEIPBQEnqPft", Mux(io.enq.fire, PopCount(io.enq.bits.pattern), 0.U))
  XSPerfAccumulate("NEIPBQOflPft", Mux(overflow,    PopCount(queue(tail).pattern), 0.U))
  XSPerfAccumulate("NEIPBQDeqPft", Mux(io.deq.fire, PopCount(io.deq.bits.pattern), 0.U))
}

class NEIPrefetch(implicit p: Parameters) extends NEIModule {
  val io = IO(new Bundle() {
    val train = Flipped(DecoupledIO(new PrefetchTrain))
    val req = DecoupledIO(new PrefetchReq)
    val resp = Flipped(DecoupledIO(new PrefetchResp))
  })

  val nei = Module(new Nei)
  val patternBufferQueue = Module(new NEIPatternBufferQueue)

  val sendingBaseAddr = RegInit(0.U(log2AddrNum.W))
  val sendingPattern  = RegInit(0.U(patternWidth.W))
  val sendingSource   = RegInit(0.U(sourceIdBits.W))
  val sendingNeedT    = RegInit(false.B)
  val patternBitCounter = Counter(patternWidth)
  val waitReqSending = RegInit(false.B)
  val prefetchReqValid = RegInit(false.B)
  val prefetchReq = RegInit(0.U.asTypeOf(new PrefetchReq))
  val sendingAddr = sendingBaseAddr + (patternBitCounter.value << log2BlockSize.U)
  val (sendingTag, sendingSet, _) = parseFullAddress(sendingAddr)

  // learning all train addr
  nei.io.req.enable := io.train.fire && io.train.bits.train_nofor_acdp
  nei.io.req.addr := io.train.bits.addr
  nei.io.req.cacheHit := io.train.bits.hit
  nei.io.req.needT := io.train.bits.needT
  nei.io.req.source := io.train.bits.source

  // enqueue when nei resp valid
  patternBufferQueue.io.enq.bits := nei.io.resp
  patternBufferQueue.io.enq.valid := nei.io.resp.valid
  
  // get the queue head when not waitReqSending
  patternBufferQueue.io.deq.ready := !waitReqSending

  io.req.valid := prefetchReqValid
  io.req.bits := prefetchReq
  io.req.bits.pfSource := MemReqSource.Prefetch2L2NEI.id.U
  io.train.ready := true.B // patternBufferQueue.io.enq.ready && (!prefetchReqValid || io.req.ready)
  io.resp.ready := true.B

  when(!waitReqSending){
    sendingPattern   := patternBufferQueue.io.deq.bits.pattern
    sendingBaseAddr  := patternBufferQueue.io.deq.bits.baseAddr
    sendingSource    := patternBufferQueue.io.deq.bits.source
    sendingNeedT     := patternBufferQueue.io.deq.bits.needT
    // if head is valid turn to waitReqSending state
    waitReqSending   := patternBufferQueue.io.deq.fire
    prefetchReqValid := false.B
  }otherwise{
    // sending all prefetchReq

    // Only the address represented by the set bit 
    // of the pattern needs to be issued
    prefetchReq.tag := sendingTag
    prefetchReq.set := sendingSet
    prefetchReq.needT := sendingNeedT
    prefetchReq.source := sendingSource
    prefetchReqValid := sendingPattern(patternBitCounter.value) === 1.U

    waitReqSending := !patternBitCounter.inc()
  }

  XSPerfAccumulate("NEI_req_total", io.req.valid)
}