package coupledL2.prefetch

import org.chipsalliance.cde.config.Parameters
import chisel3._
import chisel3.util._
import utility.{ChiselDB, Constantin, MemReqSource, ParallelPriorityMux, RRArbiterInit, SRAMTemplate, XSPerfAccumulate}

class RecentPageTableEntry(implicit p: Parameters) extends NEIBundle {
  val valid = Bool()
  val lru = UInt(log2Ceil(rpTableWayNum).W)
  val pn = UInt(log2PageNum.W)
  val pattern = UInt(patternWidth.W)
  

  def reset() = {
    valid := false.B
    lru := (rpTableWayNum - 1).U
    pn := DontCare
    pattern := DontCare
  }
}

class RecentPageTable(implicit p: Parameters) extends NEIModule {
  val io = IO(new RecentPageTableIO)

  val table = RegInit(
    VecInit.fill(rpTableWayNum)({
      val initRPEntry = Wire(new RecentPageTableEntry)
      initRPEntry.reset()
      initRPEntry
    })
  )

  def updateLru(idx: UInt) = {
    table.foreach({ e =>
      when(e.valid && e.lru < table(idx).lru) {
        e.lru := e.lru + 1.U
      }
    })
    table(idx).lru := 0.U
  }

  def isSimalarToNewPage(e: RecentPageTableEntry): Bool = {
    val distance = Mux(newPn > e.pn, newPn - e.pn,e.pn - newPn)
    val isClose = distance <= pageDistance.U
    return e.valid && e.pn =/= newPn && isClose
  }

  val enable = io.req.enable
  val cacheHit = io.req.cacheHit
  val newOffsetPattern = offsetToPattern(io.req.offset)
  val newPn = io.req.pn
  val hasMatch = table.exists(e => e.valid && e.pn === newPn)
  val matchIdx = table.indexWhere(e => e.valid && e.pn === newPn)
  val hasInvalid = table.exists(e => !e.valid)
  val invalidIdx = table.indexWhere(e => !e.valid)
  val maxLruIdx = table.indexWhere(e => e.lru === (rpTableWayNum - 1).U)
  // val victimIdx = Mux(hasInvalid, invalidIdx, maxLruIdx)
  val newPattern = Mux(hasMatch, table(matchIdx).pattern | newOffsetPattern, newOffsetPattern)
  val maxSimalarCount = table.map(e =>
    Mux(isSimalarToNewPage(e), e.pattern & newPattern, 0.U)
  ).reduce((x, y) => Mux(PopCount(x) > PopCount(y), x, y))

  val maxSimalarIdx = table.indexWhere(e =>
    (isSimalarToNewPage(e) && (e.pattern & table(matchIdx).pattern) === maxSimalarCount)
  )

  io.resp := 0.U.asTypeOf(new OutputCheckUpdateRPIO)

  when(enable) {
    when(!hasMatch) {
      when(hasInvalid){
        table(invalidIdx).valid := true.B
        table(invalidIdx).pn := newPn
        table(invalidIdx).pattern := newPattern
        updateLru(invalidIdx)
      } otherwise {
        table(maxLruIdx).valid := true.B
        table(maxLruIdx).pn := newPn
        table(maxLruIdx).pattern := newPattern
        updateLru(maxLruIdx)
      }
    }.otherwise {
      table(maxLruIdx).valid := true.B
      table(matchIdx).pattern := newPattern
      updateLru(matchIdx)
    }

    when(PopCount(maxSimalarCount) < (similarThreshold - 1).U) {
      io.resp.valid := false.B
      io.resp.pattern := 0.U
      io.resp.mask := 0.U
    }.otherwise {
      io.resp.valid := true.B
      io.resp.pattern := table(maxSimalarIdx).pattern
      io.resp.mask := newPattern
    }
  }

  XSPerfAccumulate("rptInsert", enable)
  XSPerfAccumulate("rptInsertHit", enable && hasMatch)
  XSPerfAccumulate("rptFoundSimalar", enable && io.resp.valid)
}