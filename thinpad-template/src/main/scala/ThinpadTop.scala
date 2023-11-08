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

case class AddrMapping(
    baseramAddr: UInt = "h8000_0000".U,
    baseramMsk : UInt = "hffc0_0000".U,
    extramAddr: UInt = "h8040_0000".U,
    extramMsk : UInt = "hffc0_0000".U
)

class ThinpadTop(config: ThinpadTopConfig, mmap: AddrMapping) extends RawModule {
    override val desiredName = s"lab4_top"
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
                // code
            }
        }
    }
}

object Main extends App {
    ChiselStage.emitSystemVerilogFile(
      new ThinpadTop(ThinpadTopConfig(), AddrMapping()),
      Array("--target-dir","generated"),
      firtoolOpts = Array("-disable-all-randomization", "-strip-debug-info")
    )
}