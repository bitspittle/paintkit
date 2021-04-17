package bitspittle.messagebus

import net.jcip.annotations.GuardedBy
import java.lang.ref.WeakReference
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

/** Typed class used as a key for ensuring associated listeners inherit from the type. */
class Topic<T : Any>(internal val topicClass: Class<T>) {
    companion object {
        inline fun <reified T : Any> create(): Topic<T> {
            require(T::class.java.isInterface) { "MessageBus topics can only be created for interface types." }
            return Topic(T::class.java)
        }
    }
}

/**
 * A central message hub which allows the sending of and listening to arbitrary messages, which serves as a simple
 * replacement for writing a bunch of event listeners yourself.
 *
 * Inspired by the [MessageBus from the IntelliJ codebase](https://plugins.jetbrains.com/docs/intellij/messaging-infrastructure.html),
 * this approach offers uses weak references under the hood to prevent potential memory leaks.
 *
 * To use it, create a [Topic] against some interface, and then use [withOwner] plus [TopicListeners.listenTo] to
 * register a bunch of listeners. To fire messages, call [getSender]. Behind the scenes, it generates a proxy class via
 * reflection which automatically connects calls from the sender to all listeners.
 */
@Suppress("UNCHECKED_CAST") // Types verified by topic
class MessageBus {

    private val lock = Any()

    @GuardedBy("lock")
    private val ownerListenerMap = mutableMapOf<WeakReference<*>, TopicListeners>()

    @GuardedBy("lock")
    private val topicImplMap = mutableMapOf<Topic<*>, Any>()

    @GuardedBy("lock")
    private val enqueuedInvokes = mutableListOf<() -> Unit>()

    fun <T : Any> getSender(topic: Topic<T>): T {
        return synchronized(lock) {
            topicImplMap.computeIfAbsent(topic) { topic ->
                topic.topicClass.let { topicClass ->
                    val handler = InvocationHandler { _, implMethod, implArgs ->
                        synchronized(lock) {
                            val isInvokeRunner = enqueuedInvokes.size == 0

                            ownerListenerMap.keys.removeAll { weakRef -> weakRef.get() == null }
                            ownerListenerMap.values.forEach { topicListeners ->
                                topicListeners.listeners[topic]?.let { listener ->
                                    val method = listener.javaClass.methods.single { method ->
                                        method.name == implMethod.name
                                                && method.parameterCount == implMethod.parameterCount
                                                && method.parameterTypes.contentEquals(implMethod.parameterTypes)
                                    }

                                    // Don't fire right away. This prevents listeners from responding immediately and
                                    // cutting in front of the line by having their invoke called first.
                                    enqueuedInvokes.add {
                                        method.invoke(listener, *implArgs)
                                    }
                                }
                            }

                            if (isInvokeRunner) {
                                while (true) {
                                    enqueuedInvokes.removeFirstOrNull()?.invoke() ?: break
                                }
                            }
                        }
                    }
                    Proxy.newProxyInstance(
                        topicClass.classLoader,
                        arrayOf<Class<*>>(topicClass),
                        handler
                    )
                }
            }
        } as T
    }

    fun withOwner(owner: Any): TopicListeners {
        synchronized(lock) {
            val foundEntry = ownerListenerMap.entries.singleOrNull { it.key.get() == owner }
            return if (foundEntry != null) {
                foundEntry.value
            } else {
                val topicListeners = TopicListeners()
                ownerListenerMap[WeakReference(owner)] = topicListeners
                topicListeners
            }
        }
    }

    fun forgetAll(owner: Any) {
        synchronized(lock) {
            ownerListenerMap.keys.removeAll { it.get() == null || it.get() == owner }
        }
    }

    fun forgetAll() {
        synchronized(lock) {
            ownerListenerMap.clear()
        }
    }
}

class TopicListeners {
    internal val listeners = mutableMapOf<Topic<*>, Any>()

    fun <T : Any> listenTo(topic: Topic<T>, listener: T) {
        require(!listeners.containsKey(topic)) { "Cannot register multiple listeners for a single topic. Call `forget` first if you really want to do this." }
        listeners[topic] = listener
    }

    fun <T : Any> forget(topic: Topic<T>) {
        listeners.remove(topic)
    }

}