import chisel3._
import chisel3.experimental._

class SramTester extends ExtModule(Map("ADDR_BASE" -> "32'h8000_0000",
                                        "ADDR_MASK" -> "32'h007F_FFFF")) {
    override val desiredName = s"sram_tester" 
    val clk_i = IO(Input(Clock()))
    val rst_i = IO(Input(Bool()))
    val wb_cyc_o = IO(Output(Bool()))
    val wb_stb_o = IO(Output(Bool()))
    val wb_ack_i = IO(Input(Bool()))
    val wb_adr_o = IO(Output(UInt(32.W)))
    val wb_dat_o = IO(Output(UInt(32.W)))
    val wb_dat_i = IO(Input(UInt(32.W)))
    val wb_sel_o = IO(Output(UInt(4.W)))
    val wb_we_o = IO(Output(Bool()))

    val start = IO(Input(Bool()))
    val random_seed = IO(Input(UInt(32.W)))
    val done = IO(Output(Bool()))
    val error = IO(Output(Bool()))

    val error_round = IO(Output(UInt(32.W)))
    val error_addr = IO(Output(UInt(32.W)))
    val error_read_data = IO(Output(UInt(32.W)))
    val error_expected_data = IO(Output(UInt(32.W)))
} 