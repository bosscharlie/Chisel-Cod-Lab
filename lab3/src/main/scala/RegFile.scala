import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class RegFile (regnums: Int, datawidth: Int) extends Module {
    val io = IO(new Bundle{
        val waddr = Input(UInt(log2Ceil(regnums).W))
        val wdata = Input(UInt(datawidth.W))
        val we = Input(Bool())
        val raddr_a = Input(UInt(log2Ceil(regnums).W))
        val rdata_a = Output(UInt(datawidth.W))
        val raddr_b = Input(UInt(log2Ceil(regnums).W))
        val rdata_b = Output(UInt(datawidth.W))
    })
    val regs = RegInit(VecInit.fill(regnums)(0.U(datawidth.W)))
    when(io.we) {
        regs(io.waddr) := Mux(io.waddr === 0.U, 0.U, io.wdata)
    }
    io.rdata_a := Mux(io.raddr_a === 0.U, 0.U, regs(io.raddr_a))
    io.rdata_b := Mux(io.raddr_b === 0.U, 0.U, regs(io.raddr_b))
}

object RegFileMain extends App {
    ChiselStage.emitSystemVerilogFile(
      new RegFile(32,16),
      Array("--target-dir","generated"),
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}