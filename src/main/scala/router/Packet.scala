package main.scala.router

import Chisel._

class PacketDir extends Bundle {
  val y = UInt(width = 4)
  val x = UInt(width = 4)
}

class PacketHeader extends Bundle {
  val error = Bool()
  val exop = Bool()
  val writeMask = Bits(width = 16)
  val writeReq = Bool()
  val reply = Bool()
  val address = Bits(width = Packet.ADDRESS_WIDTH)
}

class Packet extends Bundle {
  val dest = new PacketDir()
  val sender = new PacketDir()
  val payload = Bits(width = Packet.DATA_WIDTH)
  val header = new PacketHeader()

  def assign(other: Packet): Unit = {
    this.dest.y := other.dest.y
    this.dest.x := other.dest.x
    this.sender.y := other.sender.y
    this.sender.x := other.sender.x
    this.payload := other.payload
    this.header.error := other.header.error
    this.header.exop := other.header.exop
    this.header.writeMask := other.header.writeMask
    this.header.writeReq := other.header.writeReq
    this.header.reply := other.header.reply
    this.header.address := other.header.address
  }

  def init(value: UInt): Unit = {
    this.dest.y := value
    this.dest.x := value
    this.sender.y := value
    this.sender.x := value
    this.payload := value
    this.header.error := value
    this.header.exop := value
    this.header.writeMask := value
    this.header.writeReq := value
    this.header.reply := value
    this.header.address := value
  }
}

object Packet {
  val LENGTH = 100

  val Y_DEST_END          = 99
  val Y_DEST_BEGIN        = 96
  val X_DEST_END          = 95
  val X_DEST_BEGIN        = 92
  val Y_SEND_END          = 91
  val Y_SEND_BEGIN        = 88
  val X_SEND_END          = 87
  val X_SEND_BEGIN        = 84
  val DATA_END            = 83
  val DATA_BEGIN          = 52
  val ERROR_INDEX         = 51
  val EXOP_INDEX          = 50
  val WRITE_MASK_END      = 49
  val WRITE_MASK_BEGIN    = 34
  val WRITE_REQUEST_INDEX = 33
  val REPLY_INDEX         = 32
  val ADDRESS_END         = 31
  val ADDRESS_BEGIN       = 0

  val DATA_WIDTH = DATA_END - DATA_BEGIN + 1
  val ADDRESS_WIDTH  = ADDRESS_END - ADDRESS_BEGIN + 1
}
