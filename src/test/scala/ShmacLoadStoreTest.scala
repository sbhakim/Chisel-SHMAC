package test

import Chisel._
import Sodor.ShmacUnit

class ShmacLoadStoreTest(t: ShmacUnit) extends Tester(t) {

  def peekArbiter(): Unit = {
    println("#-----")
    println("# Printing arbiter signals")
    peek(t.arbiter.io.mem)
    peek(t.arbiter.io.imem.req.ready)
    peek(t.arbiter.io.imem.req.valid)
    peek(t.arbiter.io.imem.req.bits.addr)
    //    peek(t.arbiter.io.imem.req.bits.data )
    peek(t.arbiter.io.imem.req.bits.fcn)
    peek(t.arbiter.io.imem.req.bits.typ)
    peek(t.arbiter.io.imem.resp.ready)
    peek(t.arbiter.io.imem.resp.valid)
    peek(t.arbiter.io.imem.resp.bits.data)
    peek(t.arbiter.io.dmem.req.ready)
    peek(t.arbiter.io.dmem.req.valid)
    peek(t.arbiter.io.dmem.req.bits.addr)
    peek(t.arbiter.io.dmem.req.bits.data)
    peek(t.arbiter.io.dmem.req.bits.fcn)
    peek(t.arbiter.io.dmem.req.bits.typ)
    peek(t.core.io.dmem.resp.ready)
    peek(t.arbiter.io.dmem.resp.valid)
    peek(t.arbiter.io.dmem.resp.bits.data)
    println("#-----")
  }

  poke(t.io.host.reset, 1)
  poke(t.io.mem.req.ready, 0)

  step(1) // 1
  // I-type      Width         rd            LUI
  val ld_a     = (0x1 << 12) | (0x1 << 7)  | 0x37
  //              rs2           Base          Function      Addr       SW
  val sw_a     = (0x1 << 20) | (0x0 << 15) | (0x2 << 12) | (0xc << 7) | 0x23

  //             Address        Width         rd            LD
  val ld_b     = (0x0 << 20) | (0x2 << 12) | (0x2 << 7)  | 0x03
  // S-type      rs2           Base          Function      Addr        SW
  val sw_b     = (0x2 << 20) | (0x0 << 15) | (0x2 << 12) | (0xd << 7) | 0x23

  poke(t.io.host.reset, 0)

  // The processor should request the first instruction
  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1) // Gets valid after req.ready is set
  expect(t.io.mem.req.bits.fcn, 0)
  expect(t.io.mem.req.bits.typ, 7)
  expect(t.io.mem.req.bits.addr, 0x2000)
  expect(t.io.mem.req.bits.data, 0)
  poke(t.io.mem.resp.valid, 1) // Resp must be ready if req.ready is set
  poke(t.io.mem.resp.bits.data, ld_a) // Serve the first instruction
  expect(t.io.mem.resp.ready, 1) // Waiting for imem response at 0x2000

  // Check imem
  expect(t.core.io.imem.req.valid, 1)
  expect(t.core.io.imem.req.ready, 1)
  // Check dmem
  expect(t.core.io.dmem.req.valid, 0)
  expect(t.core.io.dmem.req.ready, 1)

  step(1) // 2

  expect(t.io.mem.req.valid, 0)
  poke(t.io.mem.req.ready, 0)

  step(1) // 3

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0) // Load
  expect(t.io.mem.req.bits.typ, 7) // Imem?
  expect(t.io.mem.req.bits.addr, 0x2004)
  //  expect(t.io.mem.req.bits.data, 0)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, sw_a)
  expect(t.io.mem.resp.ready, 1)

  // Check imem
  expect(t.core.io.imem.req.valid, 1)
  expect(t.core.io.imem.req.ready, 1)
  // Check dmem
  expect(t.core.io.dmem.req.valid, 0)
  expect(t.core.io.dmem.req.ready, 1)

  step(1) // 4

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 1) // Store
  expect(t.io.mem.req.bits.typ, 3) // Dmem?
  expect(t.io.mem.req.bits.addr, 0xc)
  expect(t.io.mem.req.bits.data, 0x1000)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, 0)
  expect(t.io.mem.resp.ready, 1)

  // Check imem
  expect(t.core.io.imem.req.valid, 0)
  expect(t.core.io.imem.req.ready, 0)
  // Check dmem
  expect(t.core.io.dmem.req.valid, 1)
  expect(t.core.io.dmem.req.ready, 1)

  step(1) // 5

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0)
  expect(t.io.mem.req.bits.typ, 7)
  expect(t.io.mem.req.bits.addr, 0x2008)
//  expect(t.io.mem.req.bits.data, 0x1000)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, ld_b)
  expect(t.io.mem.resp.ready, 1)

  // Check imem
  expect(t.core.io.imem.req.valid, 1)
  expect(t.core.io.imem.req.ready, 1)
  // Check dmem
  expect(t.core.io.dmem.req.valid, 0)
  expect(t.core.io.dmem.req.ready, 1)

  step(1) // 6

  expect(t.io.mem.req.valid, 0)
  poke(t.io.mem.req.ready, 0)

  step(1) // 7

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0) // Load
  expect(t.io.mem.req.bits.typ, 3) // Dmem? Valid?
  expect(t.io.mem.req.bits.addr, 0x0)
  expect(t.io.mem.req.bits.data, 0)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, 0xa)
//  poke(t.io.mem.resp.bits.addr, 0xa)
  expect(t.io.mem.resp.ready, 1)

  // Check imem
  expect(t.core.io.imem.req.valid, 0)
  expect(t.core.io.imem.req.ready, 0)
  // Check dmem
  expect(t.core.io.dmem.req.valid, 1)
  expect(t.core.io.dmem.req.ready, 1)

  peekArbiter()
  step(1) // 8

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0)
  expect(t.io.mem.req.bits.typ, 7)
  expect(t.io.mem.req.bits.addr, 0x200c)
  expect(t.io.mem.req.bits.data, 0)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, sw_b)
  expect(t.io.mem.resp.ready, 1)

  // Check imem
  expect(t.core.io.imem.req.valid, 1)
  expect(t.core.io.imem.req.ready, 1)
  // Check dmem
  expect(t.core.io.dmem.req.valid, 0)
  expect(t.core.io.dmem.req.ready, 1)

  step(1) // 9

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1) // <- bug? Dmem is requesting..
  expect(t.io.mem.req.bits.fcn, 1) // Store
  expect(t.io.mem.req.bits.typ, 3) // Dmem?
  expect(t.io.mem.req.bits.addr, 0xd)
  expect(t.io.mem.req.bits.data, 0xa)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, 0)
  expect(t.io.mem.resp.ready, 1)

  // Check imem
  expect(t.core.io.imem.req.valid, 0)
  expect(t.core.io.imem.req.ready, 0)
  // Check dmem
  expect(t.core.io.dmem.req.valid, 1)
  expect(t.core.io.dmem.req.ready, 1)

  peekArbiter()
  step(1) // 10

  poke(t.io.mem.req.ready, 1)
  expect(t.io.mem.req.valid, 1)
  expect(t.io.mem.req.bits.fcn, 0)
  expect(t.io.mem.req.bits.typ, 7)
  expect(t.io.mem.req.bits.addr, 0x2010)
  //  expect(t.io.mem.req.bits.data, 0x1000)
  poke(t.io.mem.resp.valid, 1)
  poke(t.io.mem.resp.bits.data, sw_a) // Start over...
  expect(t.io.mem.resp.ready, 1)

  // Check imem
  expect(t.core.io.imem.req.valid, 1)
  expect(t.core.io.imem.req.ready, 1)
  // Check dmem
  expect(t.core.io.dmem.req.valid, 0)
  expect(t.core.io.dmem.req.ready, 1)

  peekArbiter()
}
