package bitspittle.messagebus

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.fail

class MessageBusTest {
    interface ChatListener {
        companion object {
            val TOPIC = Topic.create<ChatListener>()
        }

        fun onMessage(toId: Int, fromId: Int, message: String)
    }

    class TestChatListener(val id: Int) : ChatListener {
        var lastMessage: String? = null
        override fun onMessage(toId: Int, fromId: Int, message: String) {
            if (toId == id) {
                lastMessage = message
            }
        }
    }

    interface TriggerListener {
        companion object {
            val TOPIC = Topic.create<TriggerListener>()
        }
        val id: Int
        fun onTriggered(fromId: Int)
    }

    class TestTriggerListener(override val id: Int, private val handleOnTriggered: (Int) -> Unit) : TriggerListener {
        override fun onTriggered(fromId: Int) {
            handleOnTriggered(id)
        }
    }

    private val messageBus = MessageBus()

    @AfterEach
    fun tearDown() {
        messageBus.forgetAll()
    }

    @Test
    fun notAllowedToCreateTopicsForNonInterfaces() {
        try {
            Topic.create<TestChatListener>()
            fail()
        }
        catch (ignored: IllegalArgumentException) {
        }
    }


    @Test
    fun testAddAndTriggerListeners() {
        val owner1 = Any()
        val owner2 = Any()
        val chatListener1 = TestChatListener(1)
        val chatListener2 = TestChatListener(2)
        messageBus.withOwner(owner1).listenTo(ChatListener.TOPIC, chatListener1)
        messageBus.withOwner(owner2).listenTo(ChatListener.TOPIC, chatListener2)

        assertThat(chatListener1.lastMessage).isNull()
        assertThat(chatListener2.lastMessage).isNull()

        messageBus.getSender(ChatListener.TOPIC).onMessage(1, 2, "Test 1")
        assertThat(chatListener1.lastMessage).isEqualTo("Test 1")
        assertThat(chatListener2.lastMessage).isNull()

        messageBus.getSender(ChatListener.TOPIC).onMessage(2, 1, "Test 2")
        assertThat(chatListener1.lastMessage).isEqualTo("Test 1")
        assertThat(chatListener2.lastMessage).isEqualTo("Test 2")

        messageBus.getSender(ChatListener.TOPIC).onMessage(1, 2, "Test 3")
        assertThat(chatListener1.lastMessage).isEqualTo("Test 3")
        assertThat(chatListener2.lastMessage).isEqualTo("Test 2")
    }

    @Test
    fun senderFindsAllAddedAndRemovedListeners() {
        val owner1 = Any()
        val owner2 = Any()
        val owner3 = Any()
        val chatListener1 = TestChatListener(0)
        val chatListener2 = TestChatListener(0)
        val chatListener3 = TestChatListener(0)

        val sender = messageBus.getSender(ChatListener.TOPIC)
        assertThat(chatListener1.lastMessage).isNull()
        assertThat(chatListener2.lastMessage).isNull()
        assertThat(chatListener3.lastMessage).isNull()

        sender.onMessage(0, 0, "Test 1")
        assertThat(chatListener1.lastMessage).isNull()
        assertThat(chatListener2.lastMessage).isNull()
        assertThat(chatListener3.lastMessage).isNull()

        messageBus.withOwner(owner1).listenTo(ChatListener.TOPIC, chatListener1)
        sender.onMessage(0, 0, "Test 2")
        assertThat(chatListener1.lastMessage).isEqualTo("Test 2")
        assertThat(chatListener2.lastMessage).isNull()
        assertThat(chatListener3.lastMessage).isNull()

        messageBus.withOwner(owner2).listenTo(ChatListener.TOPIC, chatListener2)
        sender.onMessage(0, 0, "Test 3")
        assertThat(chatListener1.lastMessage).isEqualTo("Test 3")
        assertThat(chatListener2.lastMessage).isEqualTo("Test 3")
        assertThat(chatListener3.lastMessage).isNull()

        messageBus.withOwner(owner1).forget(ChatListener.TOPIC)
        messageBus.withOwner(owner3).listenTo(ChatListener.TOPIC, chatListener3)
        sender.onMessage(0, 0, "Test 4")
        assertThat(chatListener1.lastMessage).isEqualTo("Test 3")
        assertThat(chatListener2.lastMessage).isEqualTo("Test 4")
        assertThat(chatListener3.lastMessage).isEqualTo("Test 4")
    }

    @Test
    fun notAllowedToCreateMultipleListenersToTheSameTopicWithTheSameOwner() {
        val owner = Any()
        messageBus.withOwner(owner).listenTo(ChatListener.TOPIC, TestChatListener(1))
        try {
            messageBus.withOwner(owner).listenTo(ChatListener.TOPIC, TestChatListener(2))
            fail()
        }
        catch (ignored: java.lang.IllegalArgumentException) {
        }
    }

    @Test
    fun canForgetByTopic() {
        val owner = Any()
        val listener = TestChatListener(0)
        messageBus.withOwner(owner).listenTo(ChatListener.TOPIC, listener)

        messageBus.getSender(ChatListener.TOPIC).onMessage(0, 0, "Test")
        assertThat(listener.lastMessage).isEqualTo("Test")

        messageBus.withOwner(owner).forget(ChatListener.TOPIC)

        messageBus.getSender(ChatListener.TOPIC).onMessage(0, 0, "Test 2")
        assertThat(listener.lastMessage).isEqualTo("Test")
    }

    @Test
    fun canForgetByOwner() {
        val owner = Any()
        val listener = TestChatListener(0)
        messageBus.withOwner(owner).listenTo(ChatListener.TOPIC, listener)

        messageBus.getSender(ChatListener.TOPIC).onMessage(0, 0, "Test")
        assertThat(listener.lastMessage).isEqualTo("Test")

        messageBus.forgetAll(owner)

        messageBus.getSender(ChatListener.TOPIC).onMessage(0, 0, "Test 2")
        assertThat(listener.lastMessage).isEqualTo("Test")
    }

    @Test
    fun canForgetAll() {
        val owner = Any()
        val listener = TestChatListener(0)
        messageBus.withOwner(owner).listenTo(ChatListener.TOPIC, listener)

        messageBus.getSender(ChatListener.TOPIC).onMessage(0, 0, "Test")
        assertThat(listener.lastMessage).isEqualTo("Test")

        messageBus.forgetAll()

        messageBus.getSender(ChatListener.TOPIC).onMessage(0, 0, "Test 2")
        assertThat(listener.lastMessage).isEqualTo("Test")
    }

    @Test
    fun allCurrentMessagesSentBeforeNewMessages() {

        val order = mutableListOf<Int>()

        val listener1 = TestTriggerListener(id = 1) { id -> order.add(id) }
        val listener2 = TestTriggerListener(id = 2) { id -> order.add(id) }
        val listener3 = object : TriggerListener {
            override val id: Int = 3
            override fun onTriggered(fromId: Int) {
                if (fromId == id) return // Avoid infinite recursion!
                order.add(id)
                messageBus.getSender(TriggerListener.TOPIC).onTriggered(id)
            }
        }

        messageBus.withOwner(listener1).listenTo(TriggerListener.TOPIC, listener1)
        messageBus.withOwner(listener2).listenTo(TriggerListener.TOPIC, listener2)
        messageBus.withOwner(listener3).listenTo(TriggerListener.TOPIC, listener3)

        messageBus.getSender(TriggerListener.TOPIC).onTriggered(1)

        // Order isn't guaranteed, but the first wave of messages will all get handled before second wave goes out
        assertThat(order.subList(0, 3)).containsExactly(1, 2, 3)
        assertThat(order.subList(3, 5)).containsExactly(1, 2)
    }
}