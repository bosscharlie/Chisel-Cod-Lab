import chisel3._
import chisel3.experimental._
import circt.stage.ChiselStage

// Peripherals configuration
case class ThinpadTopConfig(
    CpldUartEnable: Boolean = true,
    BaseRamEnable: Boolean = false,
    ExtRamEnable: Boolean = false,
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
    leds := 0.U
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