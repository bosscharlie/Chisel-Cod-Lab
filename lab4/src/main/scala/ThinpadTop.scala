import chisel3._
import chisel3.experimental._
import circt.stage.ChiselStage

// Peripherals configuration
case class ThinpadTopConfig(
    CpldUartEnable: Boolean = true,
    BaseRamEnable: Boolean = true,
    ExtRamEnable: Boolean = true,
    Uart0Enable: Boolean = false,
    FlashEnable: Boolean = false,
    Sl811Enable: Boolean = false,
    Dm9kEnable: Boolean = false,
    VideoEnable: Boolean = false
)

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

class ThinpadTop extends RawModule {
    override val desiredName = s"lab4_top"
    val config = ThinpadTopConfig()
    val clk_50M = IO(Input(Clock()))
    val clk_11M0592 = IO(Input(Clock()))

    val push_btn = IO(Input(Bool()))
    val reset_btn = IO(Input(Bool()))

    val touch_btn = IO(Input(UInt(4.W)))
    val dip_sw = IO(Input(UInt(32.W)))
    val leds = IO(Output(UInt(16.W)))
    val dpy0 = IO(Output(UInt(8.W)))
    val dpy1 = IO(Output(UInt(8.W)))

    val uart = if(config.CpldUartEnable) IO(new CpldUartPort) else null
    val base = if(config.BaseRamEnable) IO(new SramPort) else null
    val ext = if(config.ExtRamEnable) IO(new SramPort) else null
    val uart0 = noPrefix{if(config.Uart0Enable) new UartPort else null}
    val flash = if(config.FlashEnable) IO(new FlashPort) else null
    val sl811 = if(config.Sl811Enable) IO(new USBPort) else null
    val dm9k = if(config.Dm9kEnable) IO(new Dm9kPort) else null
    val video = if(config.VideoEnable) IO(new VGAPort) else null

    dpy0 := 0.U
    dpy1 := 0.U
    // 禁用直连串口
    uart.rdn := true.B
    uart.wrn := true.B

    val clock_gen = noPrefix{Module(new PLL())}
    clock_gen.clk_in1 := clk_50M
    clock_gen.reset := reset_btn
    val clk_10M = clock_gen.clk_out1
    val clk_20M = clock_gen.clk_out2
    val locked = clock_gen.locked

    withClock(clk_10M) {
        withReset(reset_btn) {
            val reset_of_clock10M = RegInit(false.B)
            reset_of_clock10M := Mux(locked === false.B, true.B, false.B)
            // clock domain for sys_reset
            withReset(reset_of_clock10M) {
                val u_sram_tester = noPrefix{Module(new SramTester())}
                u_sram_tester.clk_i := clk_10M
                u_sram_tester.rst_i := reset_of_clock10M
                val wb_mux = noPrefix{Module(new WishboneMux())}
                wb_mux.clk := clk_10M
                wb_mux.rst := reset_of_clock10M
                val sram_controller_base = Module(new SramController())
                sram_controller_base.io.sram_io <> base
                val sram_controller_ext = Module(new SramController())
                sram_controller_ext.io.sram_io <> ext
                // sram tester input 
                u_sram_tester.start := push_btn
                u_sram_tester.random_seed := dip_sw
                u_sram_tester.wb_ack_i := wb_mux.wbm_ack_o
                u_sram_tester.wb_dat_i := wb_mux.wbm_dat_o
                val bits = VecInit.fill(16)(false.B)
                bits(0) := u_sram_tester.done
                bits(1) := u_sram_tester.error
                leds := bits.asUInt
                // input from sram tester
                wb_mux.wbm_adr_i := u_sram_tester.wb_adr_o
                wb_mux.wbm_dat_i := u_sram_tester.wb_dat_o
                wb_mux.wbm_we_i := u_sram_tester.wb_we_o
                wb_mux.wbm_sel_i := u_sram_tester.wb_sel_o
                wb_mux.wbm_stb_i := u_sram_tester.wb_stb_o
                wb_mux.wbm_cyc_i := u_sram_tester.wb_cyc_o
                // connect to slave0
                wb_mux.wbs0_addr := "h8000_0000".U
                wb_mux.wbs0_addr_msk := "hffc0_0000".U
                wb_mux.wbs0_dat_i := sram_controller_base.io.dat_o
                wb_mux.wbs0_ack_i := sram_controller_base.io.ack_o
                wb_mux.wbs0_err_i := false.B
                wb_mux.wbs0_rty_i := false.B
                sram_controller_base.io.cyc_i := wb_mux.wbs0_cyc_o
                sram_controller_base.io.stb_i := wb_mux.wbs0_stb_o
                sram_controller_base.io.adr_i := wb_mux.wbs0_adr_o
                sram_controller_base.io.dat_i := wb_mux.wbs0_dat_o
                sram_controller_base.io.we_i := wb_mux.wbs0_we_o
                sram_controller_base.io.sel_i := wb_mux.wbs0_sel_o
                // conect to slave1
                wb_mux.wbs1_addr := "h8040_0000".U
                wb_mux.wbs1_addr_msk := "hffc0_0000".U
                wb_mux.wbs1_dat_i := sram_controller_ext.io.dat_o
                wb_mux.wbs1_ack_i := sram_controller_ext.io.ack_o
                wb_mux.wbs1_err_i := false.B
                wb_mux.wbs1_rty_i := false.B
                sram_controller_ext.io.cyc_i := wb_mux.wbs1_cyc_o
                sram_controller_ext.io.stb_i := wb_mux.wbs1_stb_o
                sram_controller_ext.io.adr_i := wb_mux.wbs1_adr_o
                sram_controller_ext.io.dat_i := wb_mux.wbs1_dat_o
                sram_controller_ext.io.we_i := wb_mux.wbs1_we_o
                sram_controller_ext.io.sel_i := wb_mux.wbs1_sel_o
            }
        }
    }
}

object Main extends App {
    ChiselStage.emitSystemVerilogFile(
      new ThinpadTop,
      Array("--target-dir","generated"),
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}