<!-- TODO: Make this nicer later. Short version follows for now. -->

* commands, responses, and events wrap byte array payloads
  * It is up to the user of this library to determine what encoding should be used for the payloads (e.g. a JSON string
    converted to bytes, or using Google protobuf)
* If the client sends a command, the server must respond with a response
* The server can send an event at any point
* Client and server must implement handlers (ClientHandler and ServerHandler) to actually handle the incoming byte
  payloads.

Here's a silly but concrete use-case: Let's say you have a server which calculates fibonacci asynchronously, on a fancy
server somewhere. It might take a while, so you allow cancelling it. You might define the following messages:

```
CalcFibCommand {
  n: Int
  timeout: Int // Secs before giving up
}

CalcFibResponse {
  id: Long // Not the answer; an ID to reference when the answer comes in
}

CancelFibCommand {
  id: Long
}

// Response alone confirms the command was received. If a value was cancelled,
// an event will be generated
CancelFibResponse {}

FibCalculatedEvent {
  id: Long
  answer: Long
}

// Sent if the calculation was cancelled or timed out
FibCancelledEvent {
  id: Long
}
```

So after setting up the client and server and implementing the handlers, you'd call it like so (with a bit of
handwaving around converting to and from byte arrays"):

```
val response = messenger.sendCommand(CalcFibCommand { n = 1234, timeout = 10}).parse()
values[response.id] = n

// Maybe user presses stop?
messenger.sendCommand(CancelFibCommand { id = response.id })
values.remove(response.id)

// In an alternate universe, good news, the answer came in:
class FibClientHandler : ClientHandler {
  override suspend fun onEvent(event: ByteArray) {
    val event = event.parse()
    println("Fib(${values[event.id]}) = ${event.answer}")
  }
}











-->