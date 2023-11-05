import chisel3._
import chisel3.experimental._
import circt.stage.ChiselStage
import counter.Counter
import trigger.Trigger

case class ThinpadTopConfig(
    CpldUartEnable: Boolean = false,
    BaseRamEnable: Boolean = false,
    ExtRamEnable: Boolean = false,
    Uart0Enable: Boolean = true,
    FlashEnable: Boolean = false,
    Sl811Enable: Boolean = false,
    Dm9kEnable: Boolean = false,
    VideoEnable: Boolean = false
)

class CpldUartPort extends Bundle {
    val rdn = IO(Output(Bool()))
    val wrn = IO(Output(Bool()))
    val dataready = IO(Input(Bool()))
    val uart_tbre = IO(Input(Bool()))
    val uart_tsre = IO(Input(Bool()))
}

class UartPort extends Bundle {
    val txd = IO(Output(Bool()))
    val rxd = IO(Input(Bool()))
}

class SramPort extends Bundle {
    val ram_data = IO(Analog(32.W))
    val ram_addr = IO(Output(UInt(20.W)))
    val ram_be_n = IO(Output(UInt(4.W)))
    val ram_ce_n = IO(Output(Bool()))
    val ram_oe_n = IO(Output(Bool()))
    val ram_we_n = IO(Output(Bool()))
}

class FlashPort extends Bundle {
    val a = IO(Output(UInt(23.W)))
    val d = IO(Analog(16.W))
    val rp_n = IO(Output(Bool()))
    val vpen = IO(Output(Bool()))
    val ce_n = IO(Output(Bool()))
    val oe_n = IO(Output(Bool()))
    val we_n = IO(Output(Bool()))
    val byte_n = IO(Output(Bool()))
}

class USBPort extends Bundle {
    val a0 = IO(Output(Bool()))
    val wr_n = IO(Output(Bool()))
    val rd_n = IO(Output(Bool()))
    val rst_n = IO(Output(Bool()))
    val dack_n = IO(Output(Bool()))
    val intrq = IO(Input(Bool()))
    val drq_n = IO(Input(Bool()))
}

class Dm9kPort extends Bundle {
    val cmd = IO(Output(Bool()))
    val sd = IO(Analog(1.W))
    val iow_n = IO(Output(Bool()))
    val ior_n = IO(Output(Bool()))
    val cs_n = IO(Output(Bool()))
    val pwrst_n = IO(Output(Bool()))
    val int = IO(Input(Bool()))
}

class VGAPort() extends Bundle {
    val red = IO(Output(UInt(3.W)))
    val green = IO(Output(UInt(3.W)))
    val blue = IO(Output(UInt(2.W)))
    val hsync = IO(Output(Bool()))
    val vsync = IO(Output(Bool()))
    val clk = IO(Output(Bool()))
    val de = IO(Output(Bool()))
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
    override val desiredName = s"lab2_top"
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

    val uart = if(config.CpldUartEnable) new CpldUartPort() else null
    val base = if(config.BaseRamEnable) new SramPort() else null
    val ext = if(config.ExtRamEnable) new SramPort() else null
    val uart0 = noPrefix{if(config.Uart0Enable) new UartPort() else null}

    val flash = if(config.FlashEnable) new FlashPort() else null
    val sl811 = if(config.Sl811Enable) new USBPort() else null
    val dm9k = if(config.Dm9kEnable) new Dm9kPort() else null
    val video = if(config.VideoEnable) new VGAPort() else null

    // 不使用内存、串口时禁用其使能信号
    // base.ram_ce_n := true.B
    // base.ram_oe_n := true.B
    // base.ram_we_n := true.B

    // ext.ram_ce_n := true.B
    // ext.ram_oe_n := true.B
    // ext.ram_we_n := true.B

    // uart.rdn := true.B
    // uart.wrn := true.B

    leds := 0.U
    dpy1 := 0.U
    uart0.txd := false.B

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
                val counter = Module(new Counter)
                val trigger = Module(new Trigger)
                trigger.button := push_btn
                counter.trigger := trigger.trigger
                val u_seg = Module(new SEG7LUT)
                u_seg.iDIG := counter.count
                dpy0 := u_seg.oSEG1
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