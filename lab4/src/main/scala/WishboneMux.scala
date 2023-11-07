import chisel3._
import chisel3.experimental._

class WishboneMux extends ExtModule {
    override val desiredName = s"wb_mux_2"
    val clk = IO(Input(Clock()))
    val rst = IO(Input(Bool()))
    // master signals
    val wbm_adr_i = IO(Input(UInt(32.W)))
    val wbm_dat_i = IO(Input(UInt(32.W)))
    val wbm_dat_o = IO(Output(UInt(32.W)))
    val wbm_we_i = IO(Input(Bool()))
    val wbm_sel_i = IO(Input(UInt(4.W)))
    val wbm_stb_i = IO(Input(Bool()))
    val wbm_ack_o = IO(Output(Bool()))
    val wbm_cyc_i = IO(Input(Bool()))
    val wbm_err_o = IO(Output(Bool()))
    val wbm_rty_o = IO(Output(Bool()))
    // slave0
    val wbs0_addr = IO(Input(UInt(32.W)))
    val wbs0_addr_msk = IO(Input(UInt(32.W)))
    val wbs0_adr_o = IO(Output(UInt(32.W)))
    val wbs0_dat_i = IO(Input(UInt(32.W)))
    val wbs0_dat_o = IO(Output(UInt(32.W)))
    val wbs0_we_o = IO(Output(Bool()))
    val wbs0_sel_o = IO(Output(UInt(4.W)))
    val wbs0_stb_o = IO(Output(Bool()))
    val wbs0_ack_i = IO(Input(Bool()))
    val wbs0_err_i = IO(Input(Bool()))
    val wbs0_rty_i = IO(Input(Bool()))
    val wbs0_cyc_o = IO(Output(Bool()))
    // slave1
    val wbs1_addr = IO(Input(UInt(32.W)))
    val wbs1_addr_msk = IO(Input(UInt(32.W)))
    val wbs1_adr_o = IO(Output(UInt(32.W)))
    val wbs1_dat_i = IO(Input(UInt(32.W)))
    val wbs1_dat_o = IO(Output(UInt(32.W)))
    val wbs1_we_o = IO(Output(Bool()))
    val wbs1_sel_o = IO(Output(UInt(4.W)))
    val wbs1_stb_o = IO(Output(Bool()))
    val wbs1_ack_i = IO(Input(Bool()))
    val wbs1_err_i = IO(Input(Bool()))
    val wbs1_rty_i = IO(Input(Bool()))
    val wbs1_cyc_o = IO(Output(Bool()))
}