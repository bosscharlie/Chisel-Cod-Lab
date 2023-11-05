import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class Controller extends Module {
    val io = IO(new Bundle {
        val trigger = Input(Bool())
        val dip_sw = Input(UInt(32.W))

        val alu_op = Output(Opcode())
        val alu_y = Input(UInt(16.W))

        val waddr = Output(UInt(5.W))
        val wdata = Output(UInt(32.W))
        val we = Output(Bool())
        val raddr_a = Output(UInt(5.W))
        val rdata_a = Input(UInt(32.W))
        val raddr_b = Output(UInt(5.W))
        val rdata_b = Input(UInt(32.W))

        val leds = Output(UInt(16.W))
    })

    val inst = RegInit(0.U(32.W))

    val is_rtype = inst(2,0) === "b001".U
    val is_poke = inst(2,0) === "b010".U && inst(6,3) === "b0001".U
    val is_peek = inst(2,0) === "b010".U && inst(6,3) === "b0010".U
    val rs1 = Mux(is_peek, inst(11,7), inst(19,15))
    val rs2 = inst(24,20)
    val rd = inst(11,7)
    val imm = inst(31,16)
    val peekled = RegInit(0.U(16.W))

    io.alu_op := Opcode(inst(6,3))
    io.raddr_a := rs1
    io.raddr_b := rs2
    io.waddr := rd
    io.wdata := Mux(is_poke, imm, io.alu_y)

    object State extends ChiselEnum {
        val Init, Decode, ReadReg, Writeback = Value
    }
    import State._

    val stateReg = RegInit(Init)
    switch(stateReg) {
        is(Init) {
            when(io.trigger) {
                stateReg := Decode
                inst := io.dip_sw
            }
        }
        is(Decode) {
            when(is_peek) {
                stateReg := ReadReg
            } .otherwise {
                stateReg := Writeback
            }
        }
        is(ReadReg) {
            stateReg := Init
            peekled := io.rdata_a
        }
        is(Writeback) {
            stateReg := Init
        }
    }
    io.we := stateReg === Writeback
    io.leds := peekled
}

object ControllerMain extends App {
    ChiselStage.emitSystemVerilogFile(
      new Controller,
      Array("--target-dir","generated"),
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}