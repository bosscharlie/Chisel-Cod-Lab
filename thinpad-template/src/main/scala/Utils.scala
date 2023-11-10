import chisel3._
import chisel3.util._
import chisel3.experimental._

class CpldUartPort extends Bundle {
    val rdn = Output(Bool())
    val wrn = Output(Bool())
    // val dataready = Input(Bool())
    // val uart_tbre = Input(Bool())
    // val uart_tsre = Input(Bool())
}

class UartPort extends Bundle {
    val txd = IO(Output(Bool()))
    val rxd = IO(Input(Bool()))
}

class SramPort extends Bundle {
    val ram_data = Analog(32.W)
    val ram_addr = Output(UInt(20.W))
    val ram_be_n = Output(UInt(4.W))
    val ram_ce_n = Output(Bool())
    val ram_oe_n = Output(Bool())
    val ram_we_n = Output(Bool())
}

class FlashPort extends Bundle {
    val a = Output(UInt(23.W))
    val d = Analog(16.W)
    val rp_n = Output(Bool())
    val vpen = Output(Bool())
    val ce_n = Output(Bool())
    val oe_n = Output(Bool())
    val we_n = Output(Bool())
    val byte_n = Output(Bool())
}

class USBPort extends Bundle {
    val a0 = Output(Bool())
    val wr_n = Output(Bool())
    val rd_n = Output(Bool())
    val rst_n = Output(Bool())
    val dack_n = Output(Bool())
    val intrq = Input(Bool())
    val drq_n = Input(Bool())
}

class Dm9kPort extends Bundle {
    val cmd = Output(Bool())
    val sd = Analog(1.W)
    val iow_n = Output(Bool())
    val ior_n = Output(Bool())
    val cs_n = Output(Bool())
    val pwrst_n = Output(Bool())
    val int = Input(Bool())
}

class VGAPort() extends Bundle {
    val red = Output(UInt(3.W))
    val green = Output(UInt(3.W))
    val blue = Output(UInt(2.W))
    val hsync = Output(Bool())
    val vsync = Output(Bool())
    val clk = Output(Bool())
    val de = Output(Bool())
}

class PLL extends ExtModule {
    override val desiredName = s"pll_example"
    val clk_in1 = IO(Input(Clock()))
    val clk_out1 = IO(Output(Clock()))
    val clk_out2 = IO(Output(Clock()))
    val reset = IO(Input(Bool()))
    val locked = IO(Output(Bool()))
}

class SEG7LUT extends ExtModule {
    override val desiredName = s"SEG7_LUT"
    val iDIG = IO(Input(UInt(4.W)))
    val oSEG1 = IO(Output(UInt(8.W)))
}

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