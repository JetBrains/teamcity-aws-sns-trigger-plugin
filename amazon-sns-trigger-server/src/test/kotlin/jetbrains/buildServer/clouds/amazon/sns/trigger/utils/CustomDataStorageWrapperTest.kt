package jetbrains.buildServer.clouds.amazon.sns.trigger.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.mockk.*
import jetbrains.buildServer.clouds.amazon.sns.trigger.dto.SnsNotificationDto
import jetbrains.buildServer.clouds.amazon.sns.trigger.utils.parameters.AwsSnsTriggerConstants
import jetbrains.buildServer.serverSide.CustomDataStorage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CustomDataStorageWrapperTest {

    private lateinit var customDataStorageMock: CustomDataStorage
    private lateinit var customDataStorageWrapper: CustomDataStorageWrapper

    private val objectMapper = ObjectMapper()
        .registerModule(ParameterNamesModule())
        .registerModule(Jdk8Module())
        .registerModule(JavaTimeModule())

    @BeforeEach
    fun setUp() {
        customDataStorageMock = mockk(relaxed = true)
        every { customDataStorageMock.getValues() } returns emptyMap()
        customDataStorageWrapper = CustomDataStorageWrapper(customDataStorageMock)
    }

    @Test
    fun `test putValue calls putValue on customDataStorage and flush`() {
        val key = "testKey"
        val value = "testValue"

        customDataStorageWrapper.putValue(key, value)

        verify { customDataStorageMock.putValue(key, value) }
        verify { customDataStorageMock.flush(any()) }
    }

    @Test
    fun `test getValue returns value from customDataStorage`() {
        val key = "testKey"
        val value = "testValue"

        every { customDataStorageMock.getValue(key) } returns value

        val retrievedValue = customDataStorageWrapper.getValue(key)
        assertEquals(value, retrievedValue)

        verify { customDataStorageMock.getValue(key) }
    }

    @Test
    fun `test putValues calls putValues on customDataStorage and flush`() {
        val values = mapOf("key1" to "value1", "key2" to "value2")

        customDataStorageWrapper.putValues(values)

        verify { customDataStorageMock.putValues(values) }
        verify { customDataStorageMock.flush(any()) }
    }

    @Test
    fun `test getValues returns values from customDataStorage`() {
        val values = mapOf("key1" to "value1", "key2" to "value2")

        every { customDataStorageMock.getValues() } returns values

        val retrievedValues = customDataStorageWrapper.getValues()

        assertEquals(values, retrievedValues)
        verify { customDataStorageMock.getValues() }
    }

    @Test
    fun `test getValuesSafe returns empty map when getValues returns null`() {
        every { customDataStorageMock.getValues() } returns null

        val values = customDataStorageWrapper.getValuesSafe()

        assertTrue(values.isEmpty())
    }

    @Test
    fun `test putValue with null value`() {
        val key = "testKey"
        val value: String? = null

        customDataStorageWrapper.putValue(key, value)

        verify { customDataStorageMock.putValue(key, null) }
        verify { customDataStorageMock.flush(any()) }
    }

    @Test
    fun `test putValues with null values`() {
        val values = mapOf("key1" to "value1", "key2" to null)

        customDataStorageWrapper.putValues(values)

        verify { customDataStorageMock.putValues(values) }
        verify { customDataStorageMock.flush(any()) }
    }

    @Test
    fun `test flush retries up to 10 times when flush fails`() {
        var flushCallCount = 0

        every { customDataStorageMock.flush(any()) } answers {
            flushCallCount++
            throw RuntimeException("Flush failed")
        }

        customDataStorageWrapper.flush()

        // Verify that flush was called 11 times (initial try + 10 retries)
        assertEquals(11, flushCallCount)
    }

    @Test
    fun `test flush retries and succeeds after retries`() {
        var flushCallCount = 0

        every { customDataStorageMock.flush(any()) } answers {
            flushCallCount++
            if (flushCallCount < 5) {
                throw RuntimeException("Flush failed")
            }
            // Succeeds on 5th attempt
        }

        customDataStorageWrapper.flush()

        // Verify that flush was called 5 times
        assertEquals(5, flushCallCount)
    }

    @Test
    fun `test safeFlush updates state after failure`() {
        val key = "testKey"
        val value = "testValue"

        // Set up the initial state
        customDataStorageWrapper.putValue(key, value)

        var flushCallCount = 0

        every { customDataStorageMock.flush(any()) } answers {
            flushCallCount++
            throw RuntimeException("Flush failed")
        }

        val storageValues = mutableMapOf<String, String>("existingKey" to "existingValue")
        every { customDataStorageMock.getValues() } returns storageValues

        // Capture the values passed to putValues
        val putValuesSlot = mutableListOf<Map<String, String>>()
        every { customDataStorageMock.putValues(capture(putValuesSlot)) } answers {
            storageValues.putAll(putValuesSlot.last())
        }

        // Attempt to flush
        customDataStorageWrapper.flush()

        // Ensure that the merged state includes both existing storage values and new state
        val expectedMergedValues = mapOf(
            "existingKey" to "existingValue",
            "testKey" to "testValue"
        )

        assertEquals(expectedMergedValues, storageValues)
    }

    @Test
    fun `test conflict resolution during flush`() {
        val key = "conflictKey"
        val localValue = "localValue"
        val storageValue = "storageValue"

        // Set up local state
        customDataStorageWrapper.putValue(key, localValue)

        // Simulate a conflict during flush
        every { customDataStorageMock.flush(any()) } throws RuntimeException("Conflict during flush") andThenJust Runs

        // Simulate storage having a different value
        every { customDataStorageMock.getValues() } returns mapOf(key to storageValue)

        // Capture the merged values
        val mergedValuesSlot = slot<Map<String, String>>()
        every { customDataStorageMock.putValues(capture(mergedValuesSlot)) } just Runs

        // Attempt to flush
        customDataStorageWrapper.flush()

        // Verify conflict resolution was applied
        val expectedMergedValues = mapOf(key to localValue)
        assertEquals(expectedMergedValues, mergedValuesSlot.captured)

        verify(exactly = 1) { customDataStorageMock.putValues(any()) }
        verify(exactly = 3) { customDataStorageMock.flush(any()) }
    }

    @Test
    fun `test conflict resolution for TRIGGER_STORE_MESSAGES with valid JSON messages`() {
        val key = AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES

        // Create initial messages state
        val initialMessages = mapOf(
            "msg1" to SnsNotificationDto().apply { messageId = "Message 1" },
            "msg2" to SnsNotificationDto().apply { messageId = "Message 2"}
        )

        // Convert initial messages to JSON
        val initialMessagesJson = objectMapper.writeValueAsString(initialMessages)

        // Mock customDataStorage to return initial messages state
        every { customDataStorageMock.getValues() } returns mapOf(key to initialMessagesJson)

        // Create the wrapper (this will read the initial messages state)
        customDataStorageWrapper = CustomDataStorageWrapper(customDataStorageMock)

        // Now, simulate modifications to the messages in the wrapper
        // For example, delete "msg1" and add "msg3"
        val localMessages = mapOf(
            "msg2" to SnsNotificationDto().apply { messageId = "Message 2" },
            "msg3" to SnsNotificationDto().apply { messageId = "Message 3" }
        )
        val localMessagesJson = objectMapper.writeValueAsString(localMessages)

        // change return value to simulate successful putValue
        every { customDataStorageMock.getValues() } returns mapOf(key to localMessagesJson)
        // Put the modified messages into the wrapper
        customDataStorageWrapper.putValue(key, localMessagesJson)

        // Simulate that during flush, a conflict occurs
        every { customDataStorageMock.flush(any()) } throws RuntimeException("Conflict during flush") andThenJust Runs

        // Simulate that storage has a conflicting messages state
        val storageMessages = mutableMapOf(
            "msg1" to SnsNotificationDto().apply { messageId = "Message 1" },
            "msg2" to SnsNotificationDto().apply { messageId = "Updated Message 2"},
            "msg4" to SnsNotificationDto().apply { messageId = "Message 4" }
        )
        val storageMessagesJson = objectMapper.writeValueAsString(storageMessages)
        every { customDataStorageMock.getValues() } returns mapOf(key to storageMessagesJson)

        // Capture the merged values
        val mergedValuesSlot = slot<Map<String, String>>()
        every { customDataStorageMock.putValues(capture(mergedValuesSlot)) } just Runs

        // Attempt to flush
        customDataStorageWrapper.flush()

        // Expected merged messages after conflict resolution
        // - Remove "msg1" (deleted in local)
        // - Add "msg3" (added in local)
        // - "msg2" should remain as in storage (since local didn't change it)
        // - "msg4" remains as is
        val expectedMergedMessages = mutableMapOf(
            "msg2" to storageMessages["msg2"]!!,
            "msg3" to localMessages["msg3"]!!,
            "msg4" to storageMessages["msg4"]!!
        ).toSortedMap()

        // Verify that the merged value in the slot matches expected
        val actualMergedValues = mergedValuesSlot.captured
        val parsedActualMergedValues = objectMapper.readValue(actualMergedValues[key], object : TypeReference<HashMap<String, SnsNotificationDto?>>() {
        }).toSortedMap()
        assertEquals(expectedMergedMessages, parsedActualMergedValues)

        verify(exactly = 1) { customDataStorageMock.putValues(any()) }
    }

    @Test
    fun `test conflict resolution for TRIGGER_STORE_MESSAGES with invalid JSON messages`() {
        val key = AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES

        // Create invalid JSON for local messages
        val localMessagesJson = "invalid json"

        // Mock customDataStorage to return invalid initial messages
        every { customDataStorageMock.getValues() } returns mapOf(key to "invalid initial json")

        // Create the wrapper (this will attempt to parse invalid initial messages)
        customDataStorageWrapper = CustomDataStorageWrapper(customDataStorageMock)

        every { customDataStorageMock.getValues() } returns mapOf(key to localMessagesJson)
        // Put the invalid JSON into the wrapper
        customDataStorageWrapper.putValue(key, localMessagesJson)

        // Simulate that during flush, a conflict occurs
        every { customDataStorageMock.flush(any()) } throws RuntimeException("Conflict during flush") andThenJust Runs

        // Simulate that storage has a conflicting messages state with invalid JSON
        val storageMessagesJson = "also invalid json"
        every { customDataStorageMock.getValues() } returns mapOf(key to storageMessagesJson)

        // Capture the merged values
        val mergedValuesSlot = slot<Map<String, String>>()
        every { customDataStorageMock.putValues(capture(mergedValuesSlot)) } just Runs

        // Attempt to flush
        customDataStorageWrapper.flush()

        // Since the JSON is invalid, the code should catch exceptions and return localValue
        val actualMergedValues = mergedValuesSlot.captured
        assertEquals(localMessagesJson, actualMergedValues[key])

        verify(exactly = 1) { customDataStorageMock.putValues(any()) }
    }

    @Test
    fun `test getInitialMessagesState fails to parse invalid JSON`() {
        val key = AwsSnsTriggerConstants.TRIGGER_STORE_MESSAGES

        // Mock customDataStorage to return invalid JSON
        every { customDataStorageMock.getValues() } returns mapOf(key to "invalid json")

        // Create the wrapper
        customDataStorageWrapper = CustomDataStorageWrapper(customDataStorageMock)

        // Since we cannot access private fields directly, we assume no exceptions are thrown
        // and the wrapper is created successfully
        // The initial messages state should be empty due to parsing failure
        // We can indirectly test this by checking the behavior during conflict resolution

        // Attempt to put a value and flush
        val localMessages = mapOf(
            "msg1" to SnsNotificationDto().apply { messageId = "Message 1" }
        )
        val localMessagesJson = objectMapper.writeValueAsString(localMessages)
        every { customDataStorageMock.getValues() } returns mapOf(key to localMessagesJson)
        customDataStorageWrapper.putValue(key, localMessagesJson)

        // Simulate flush failure to trigger conflict resolution
        every { customDataStorageMock.flush(any()) } throws RuntimeException("Conflict during flush") andThenJust Runs

        // Simulate storage with invalid JSON
        every { customDataStorageMock.getValues() } returns mapOf(key to "another invalid json")

        // Capture the merged values
        val mergedValuesSlot = slot<Map<String, String>>()
        every { customDataStorageMock.putValues(capture(mergedValuesSlot)) } just Runs

        // Attempt to flush
        customDataStorageWrapper.flush()

        // Verify that the local value is used due to parsing failures
        val actualMergedValues = mergedValuesSlot.captured
        assertEquals(localMessagesJson, actualMergedValues[key])

        verify(exactly = 1) { customDataStorageMock.putValues(any()) }
    }
}
