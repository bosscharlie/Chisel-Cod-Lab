import chisel3._
import chisel3.experimental._
import circt.stage.ChiselStage
import bus._

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

    def connectSlaveToMux(muxSlavePort: WbMuxSlavePort, slavePort: WishboneSlavePort): Unit = {
        muxSlavePort.dat_i := slavePort.dat_o
        muxSlavePort.ack_i := slavePort.ack_o
        muxSlavePort.err_i := false.B
        muxSlavePort.rty_i := false.B
        slavePort.cyc_i := muxSlavePort.cyc_o
        slavePort.stb_i := muxSlavePort.stb_o
        slavePort.adr_i := muxSlavePort.adr_o
        slavePort.dat_i := muxSlavePort.dat_o
        slavePort.we_i  := muxSlavePort.we_o
        slavePort.sel_i := muxSlavePort.sel_o
    }

    def connectMasterToMux(muxMasterPort: WbMuxMasterPort, masterPort: WishboneMatserPort): Unit = {
        masterPort.ack_i := muxMasterPort.ack_o
        masterPort.dat_i := muxMasterPort.dat_o
        muxMasterPort.adr_i := masterPort.adr_o
        muxMasterPort.dat_i := masterPort.dat_o
        muxMasterPort.we_i  := masterPort.we_o
        muxMasterPort.sel_i := masterPort.sel_o
        muxMasterPort.stb_i := masterPort.stb_o
        muxMasterPort.cyc_i := masterPort.cyc_o
    }

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
                val bits = VecInit.fill(16)(false.B)
                bits(0) := u_sram_tester.done
                bits(1) := u_sram_tester.error
                leds := bits.asUInt
                // connect tester to mux
                connectMasterToMux(wb_mux.wbm, u_sram_tester.wb)
                // connect to slave0 baseram
                wb_mux.wbs0.addr := mmap.baseramAddr
                wb_mux.wbs0.addr_msk := mmap.baseramMsk
                connectSlaveToMux(wb_mux.wbs0, sram_controller_base.io.wb)
                // conect to slave1 extram
                wb_mux.wbs1.addr := mmap.extramAddr
                wb_mux.wbs1.addr_msk := mmap.extramMsk
                connectSlaveToMux(wb_mux.wbs1, sram_controller_ext.io.wb)
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