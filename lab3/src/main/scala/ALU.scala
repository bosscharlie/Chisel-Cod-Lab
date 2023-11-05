import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

object Opcode extends ChiselEnum {
    val ADD = Value(1.U)
    val SUB = Value(2.U)
    val AND = Value(3.U)
    val OR  = Value(4.U)
    val XOR = Value(5.U)
    val NOT = Value(6.U)
    val SLL = Value(7.U)
    val SRL = Value(8.U) 
    val SRA = Value(9.U) 
    val ROL = Value(10.U)
}

class ALU(datawidth: Int) extends RawModule {
    val io = IO(new Bundle {
        val a = Input(UInt(datawidth.W))
        val b = Input(UInt(datawidth.W))
        val op = Input(Opcode())
        val y = Output(UInt(datawidth.W))
    })
    io.y := io.a + io.b
    switch(io.op) {
        is(Opcode.ADD) { io.y := io.a + io.b }
        is(Opcode.SUB) { io.y := io.a - io.b }
        is(Opcode.AND) { io.y := io.a & io.b }
        is(Opcode.OR)  { io.y := io.a | io.b }
        is(Opcode.XOR) { io.y := io.a ^ io.b }
        is(Opcode.NOT) { io.y := ~io.a}
        is(Opcode.SLL) { io.y := io.a << io.b(log2Ceil(datawidth)-1,0) }
        is(Opcode.SRL) { io.y := io.a >> io.b(log2Ceil(datawidth)-1,0) }
        is(Opcode.SRA) { io.y := ((io.a.asSInt) >> io.b(log2Ceil(datawidth)-1,0)).asUInt }
        is(Opcode.ROL) { io.y := (io.a << io.b(log2Ceil(datawidth)-1,0)) + (io.a >> (datawidth.U-io.b(log2Ceil(datawidth)-1,0))) }
    }
}

object AluMain extends App {
    ChiselStage.emitSystemVerilogFile(
      new ALU(16),
      Array("--target-dir","generated"),
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}