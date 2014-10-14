package router

import Chisel._

class Router extends Module {
  val numPorts = 1
  val io = new Bundle {
    val inRequests = UInt(INPUT, width = numPorts)
    val inReady = UInt(INPUT, width = numPorts)
    val inData = Vec.fill(numPorts) { PacketData(INPUT) }
    val outRequests = UInt(OUTPUT, width = numPorts)
    val outReady = UInt(OUTPUT, width = numPorts)
    val outData = Vec.fill(numPorts) { PacketData(OUTPUT) }
  }

  val inEast = Module(new InputPort(4))
  inEast.io.fifo.in.bits := io.inData(0)
  inEast.io.fifo.in.valid := Bool(true)
  inEast.io.fifo.out.ready := Bool(true)

  val outEast = Module(new OutputPort(4))
  outEast.io.fifo.in.bits := inEast.io.fifo.out.bits
  outEast.io.fifo.in.valid := Bool(true)
  outEast.io.fifo.out.ready := Bool(true)

  io.outData(0) := outEast.io.fifo.out.bits

  // val inNorth = Module(new InputPort(4))
  // inNorth.io.fifo.in.bits := io.inData(1)
  // val outNorth = Module(new OutputPort(4))
  // io.outData(1) := outNorth.io.fifo.out.bits
}

class RouterTest(r: Router) extends Tester(r) {
  // Test to see that data travels through the router in one cycle
  // Initialize router input data in east direction
  poke(r.io.inData(0), PacketData.create(address = 10).litValue())

  // Cycle 0: Data arrives router and input port
  val routerIn = peek(r.io.inData(0))
  expect(r.inEast.io.fifo.in.bits, routerIn)
  expect(r.inEast.io.fifo.out.bits, 0)
  step(1)
  // Cycle 1: Data is at head in input port and traverses to input of
  // the output port
  val inEastOut = peek(r.inEast.io.fifo.out.bits)
  expect(r.outEast.io.fifo.in.bits, inEastOut)
  expect(r.outEast.io.fifo.out.bits, 0)
  // Cycle 2: Data reaches the output of the output port (to send it
  // further on to the network)
  step(1)
  val outEastOut = peek(r.outEast.io.fifo.out.bits)
  expect(r.io.outData(0), outEastOut)
}
