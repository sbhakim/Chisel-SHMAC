package test

import Chisel._
import router._

class OutputPortTest(p: OutputPort) extends Tester(p) {
  def testFifoIntegration() = {
    poke(p.io.fifo.in.valid, 1)
    expect(p.io.fifo.in.ready, 1)
    poke(p.io.fifo.in.bits, 10)
    step(1)
    poke(p.io.fifo.in.bits, 100)
    expect(p.io.fifo.out.bits, 10)
    step(1)
    expect(p.io.fifo.in.ready, 0)
    poke(p.io.fifo.out.ready, 1)
    expect(p.io.fifo.out.bits, 10)
    step(1)
    expect(p.io.fifo.out.bits, 100)
  }

  testFifoIntegration()
}
