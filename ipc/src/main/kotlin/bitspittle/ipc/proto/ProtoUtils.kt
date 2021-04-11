package bitspittle.ipc.proto

import bitspittle.ipc.client.ClientSocketTarget
import bitspittle.ipc.proto.IpcProto.*
import bitspittle.ipc.server.ServerSocketTarget
import bitspittle.ipc.server.ServerSocketTargets
import com.google.protobuf.ByteString
import java.nio.ByteBuffer
import java.util.*

fun ByteArray.toByteString(): ByteString {
    return ByteString.copyFrom(this)
}

internal fun UUID.toId(): Id {
    val buffer = ByteBuffer.wrap(ByteArray(16));
    buffer.putLong(mostSignificantBits)
    buffer.putLong(leastSignificantBits)

    return Id.newBuilder()
        .setValue(buffer.array().toByteString())
        .build()
}

internal fun Id.toUUID(): UUID {
    val buffer = ByteBuffer.wrap(value.toByteArray())
    return UUID(buffer.long, buffer.long)
}

internal fun buildClientMessage(block: ClientMessage.Builder.() -> Unit): ClientMessage {
    return ClientMessage.newBuilder()
        .apply(block)
        .build()
}

internal fun buildServerMessage(block: ServerMessage.Builder.() -> Unit): ServerMessage {
    return ServerMessage.newBuilder()
        .apply(block)
        .build()
}

fun ClientSocketTarget.send(clientBytes: ClientMessage) {
    if (!isConnected) return
    send(clientBytes.toByteArray())
}

fun ServerSocketTarget.send(serverBytes: ServerMessage) {
    if (!isConnected) return
    send(serverBytes.toByteArray())
}

fun ServerSocketTargets.sendAll(serverBytes: ServerMessage, filter: (ServerSocketTarget) -> Boolean = { true }) {
    sendAll(serverBytes.toByteArray(), filter)
}
