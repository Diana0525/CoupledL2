package coupledL2.prefetch

import utility.{MemReqSource, SRAMTemplate}
import org.chipsalliance.cde.config.Parameters
import chisel3._
import chisel3.util._
import coupledL2.HasCoupledL2Parameters
import utility.{ChiselDB, Constantin, MemReqSource, ParallelPriorityMux, RRArbiterInit, SRAMTemplate, XSPerfAccumulate}

case class NEIParameters(
    rpTableWayNum:   Int = 256,
    fteOffsetNum:    Int =   3,
    actRetireWin:    Int =   8,
    pageDistance:    Int =  64,
    timeStampWidth:  Int =  12,
    setBits:         Int =   8,
    similarThreshold:Int =   8,
    patternBufferQueueSize: Int =  16
) extends PrefetchParameters {
  override val hasPrefetchBit: Boolean = true
  override val hasPrefetchSrc: Boolean = true
  override val inflightEntries: Int = 16
}

trait HasNEIParams extends HasCoupledL2Parameters {
  def neiParams = prefetchers.find {
        case p: NEIParameters => true
        case _ => false
    }.get.asInstanceOf[NEIParameters]

  // Heterogenous Hardware Prefetcher
  def log2SetNum    = neiParams.setBits
  def rpTableWayNum = neiParams.rpTableWayNum
  def fteOffsetNum = neiParams.fteOffsetNum
  def actRetireWin = neiParams.actRetireWin
  def pageDistance = neiParams.pageDistance
  def timeStampWidth = neiParams.timeStampWidth
  def timeStampMask = (1 << timeStampWidth) - 1
  def similarThreshold = neiParams.similarThreshold
  def patternBufferQueueSize = neiParams.patternBufferQueueSize

  def log2AddrNum = fullAddressBits
  def log2PageSize = pageOffsetBits
  def log2PageNum = fullAddressBits - pageOffsetBits
  def log2BlockSize = offsetBits
  def log2BlockNum = pageOffsetBits - offsetBits
  def pageRegionMask = (1 << pageOffsetBits) - 1
  def patternWidth = 1 << (pageOffsetBits - offsetBits)

  assert(log2SetNum <= log2PageNum, "nei set number should be less than page number")

  def offsetToPattern(offset: UInt): UInt = {
    (1.U << offset(log2BlockNum - 1, 0)).asTypeOf(UInt(patternWidth.W))
  }
}

abstract class NEIBundle(implicit val p: Parameters) extends Bundle with HasNEIParams
abstract class NEIModule(implicit val p: Parameters) extends Module with HasNEIParams