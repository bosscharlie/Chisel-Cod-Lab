import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

// Use Analog and inline verilog to implement tri-state gate
class TriStateGate extends BlackBox with HasBlackBoxInline {
    val io = IO( new Bundle{
        val triData = Analog(32.W)
        val dataz = Input(Bool())
        val datain = Input(UInt(32.W))
        val dataout = Output(UInt(32.W))
    })
    setInline("TriStateGate.v",
        s"""
        |module TriStateGate(triData, dataz, datain, dataout);
        |inout [31:0] triData;
        |input dataz;
        |input [31:0] datain;
        |output [31:0] dataout;
        |
        |assign triData = dataz ? 32'bz : datain;
        |assign dataout = triData;
        |
        |endmodule
        """.stripMargin)
}

class SramController extends Module {
    val io = IO(new Bundle {
        val stb_i = Input(Bool())
        val cyc_i = Input(Bool())
        val ack_o = Output(Bool())
        val adr_i = Input(UInt(32.W))
        val we_i = Input(Bool())
        val dat_i = Input(UInt(32.W))
        val sel_i = Input(UInt(4.W))
        val dat_o = Output(UInt(32.W))
        val sram_io = new SramPort()
    })
    object State extends ChiselEnum {
        val IDLE, READ, READ2, WRITE, WRITE2, WRITE3, DONE = Value
    }
    import State._
    val stateReg = RegInit(IDLE)
    val rdData = RegInit(0.U(32.W))
    val dataz = WireDefault(true.B)
    dataz := stateReg === READ || stateReg === READ2
    val tri = Module(new TriStateGate)
    tri.io.triData <> io.sram_io.ram_data
    tri.io.dataz := dataz
    tri.io.datain := io.dat_i
    switch(stateReg) {
        is(IDLE) {
            when(io.stb_i && io.cyc_i) {
                when(io.we_i) {
                    stateReg := WRITE
                } .otherwise {
                    stateReg := READ
                }
            }
        }
        is(READ) { stateReg := READ2 }
        is(READ2) {
            stateReg := DONE
            rdData := tri.io.dataout
        }
        is(WRITE) { stateReg := WRITE2 }
        is(WRITE2) { stateReg := WRITE3 }
        is(WRITE3) { stateReg := DONE }
        is(DONE) { stateReg := IDLE }
    }
    io.ack_o := stateReg === DONE
    io.dat_o := rdData
    io.sram_io.ram_addr := io.adr_i(21,2)
    io.sram_io.ram_be_n := ~io.sel_i
    io.sram_io.ram_ce_n := stateReg === IDLE || stateReg === DONE
    io.sram_io.ram_oe_n := stateReg =/= READ && stateReg =/= READ2
    io.sram_io.ram_we_n := stateReg =/= WRITE2 
}

object SramMain extends App {
    ChiselStage.emitSystemVerilogFile(
      new SramController,
      Array("--target-dir","generated"),
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}