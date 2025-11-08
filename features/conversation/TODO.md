# TODO: Conversation Feature

## Completed Tasks ✅

### 2025-11-08: Fix Committee Mode Message Display Issue

**Problem:**
Messages were not displaying in Committee mode - only "Думаю..." (Thinking...) spinner was shown indefinitely.

**Root Cause:**
In `ConversationViewModel.kt:160`, the code tried to get the ID of the last user message BEFORE the new message was saved to the database:

```kotlin
val lastUserMessageId = messages.lastOrNull { it.role == Role.USER }?.id ?: 0L
```

This caused expert opinions to be linked to the OLD message instead of the NEW one.

**Solution:**

1. **Added new method to `ConversationRepository`** (`llm-domain/src/.../repository/ConversationRepository.kt:44-49`):
   ```kotlin
   suspend fun saveUserMessage(
       conversationId: String,
       message: String,
       provider: LlmProvider
   ): Long
   ```
   This method only saves the user message and returns its ID without sending a request to LLM.

2. **Implemented method in `ConversationRepositoryImpl`** (`llm-data/src/.../repository/ConversationRepositoryImpl.kt:403-420`)

3. **Updated `ExecuteCommitteeUseCase`** (`llm-domain/src/.../usecase/ExecuteCommitteeUseCase.kt:51-63`):
   - Now saves user message first and gets its ID
   - Uses this ID to link expert opinions
   - Removed `messageId` parameter from method signature

4. **Simplified `ConversationViewModel`** (`features/conversation/src/.../ConversationViewModel.kt:156-163`):
   - Removed code to get `lastUserMessageId`
   - Removed `messageId` parameter from `executeCommitteeUseCase` call

**Flow now:**
1. User enters message "Hello"
2. `ExecuteCommitteeUseCase` saves message to DB and gets ID (e.g., 6)
3. For each expert:
   - Send request to LLM
   - Save expert opinion linked to messageId = 6
4. Flow `getMessagesWithExpertOpinions` automatically picks up new opinions and updates UI
5. After all experts, final synthesis is created

**Files changed:**
- `llm-domain/src/commonMain/kotlin/ru.llm.agent/repository/ConversationRepository.kt`
- `llm-data/src/commonMain/kotlin/ru.llm.agent/repository/ConversationRepositoryImpl.kt`
- `llm-domain/src/commonMain/kotlin/ru.llm.agent/usecase/ExecuteCommitteeUseCase.kt`
- `features/conversation/src/commonMain/kotlin/ru/llm/agent/compose/presenter/ConversationViewModel.kt`

**Status:** ✅ Completed and verified with build

---

## Future Improvements

### UI/UX
- [ ] Add loading state per expert (show which expert is currently thinking)
- [ ] Add animation when expert opinions appear
- [ ] Allow collapsing/expanding expert opinions
- [ ] Add expert avatar/icon customization

### Performance
- [ ] Implement caching for expert opinions
- [ ] Add pagination for long conversations with many expert opinions
- [ ] Optimize database queries for expert opinions flow

### Features
- [ ] Allow users to customize expert system prompts
- [ ] Add ability to save/load expert configurations
- [ ] Implement expert voting/consensus mechanism
- [ ] Add ability to request clarification from specific expert

### Code Quality
- [ ] Add unit tests for ExecuteCommitteeUseCase
- [ ] Add integration tests for committee flow
- [ ] Refactor ConversationViewModel to reduce complexity
- [ ] Add documentation for committee mode architecture

---

## Known Issues

None at the moment.

---

## Notes

- Committee mode uses separate conversation IDs for each expert (`$conversationId-expert-${expert.id}`)
- Synthesis uses separate conversation ID (`$conversationId-synthesis`)
- Expert opinions are stored in separate table and linked to user messages via `messageId`
- Flow-based architecture ensures UI updates automatically when opinions are saved
