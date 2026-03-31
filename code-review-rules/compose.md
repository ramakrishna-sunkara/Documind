# Compose Rules

Rules enforcing Jetpack Compose best practices for DocuMind.

## COMPOSE-001: State Hoisting
- **Severity**: error
- **Pattern**: `@Composable\s+fun\s+\w+Screen[^{]*\{[^}]*var\s+\w+\s+by\s+remember\s*\{\s*mutableStateOf`
- **Message**: Screen-level state should be hoisted to ViewModel, not held in composables.
- **Fix**: Move state to ViewModel and pass as parameter.

### Bad
```kotlin
@Composable
fun ChatScreen() {
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var inputText by remember { mutableStateOf("") }
}
```

### Good
```kotlin
@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendMessage: (String) -> Unit
)
```

---

## COMPOSE-002: Stable Annotations
- **Severity**: warning
- **Pattern**: `data class \w+\([^)]*\)\s*(?!.*@(Stable|Immutable))`
- **Message**: Domain models used in Compose should be marked @Stable or @Immutable.
- **Fix**: Add @Stable annotation for mutable models, @Immutable for immutable ones.

### Bad
```kotlin
data class ChatMessage(
    val id: String,
    val content: String,
    val timestamp: Long
)
```

### Good
```kotlin
@Immutable
data class ChatMessage(
    val id: String,
    val content: String,
    val timestamp: Long
)
```

---

## COMPOSE-003: Remember Lambdas
- **Severity**: warning
- **Pattern**: `\w+\s*=\s*\{[^}]+\}(?!\s*\))`
- **Message**: Lambdas passed to child composables should use remember to prevent recomposition.
- **Fix**: Wrap lambda in remember { } or use method reference.

### Bad
```kotlin
@Composable
fun ParentScreen(viewModel: ViewModel) {
    ChildComponent(
        onClick = { viewModel.handleClick() }
    )
}
```

### Good
```kotlin
@Composable
fun ParentScreen(viewModel: ViewModel) {
    val onClick = remember { { viewModel.handleClick() } }
    ChildComponent(onClick = onClick)
}
```

---

## COMPOSE-004: Material Theme Usage
- **Severity**: warning
- **Pattern**: `Color\s*\(\s*0x[A-Fa-f0-9]+\s*\)|Color\.[A-Z]\w+`
- **Message**: Use MaterialTheme.colorScheme instead of hardcoded colors.
- **Fix**: Replace with MaterialTheme.colorScheme values.

### Bad
```kotlin
Text(
    text = "Hello",
    color = Color(0xFF1976D2)
)
```

### Good
```kotlin
Text(
    text = "Hello",
    color = MaterialTheme.colorScheme.primary
)
```

---

## COMPOSE-005: Derived State
- **Severity**: info
- **Pattern**: `val\s+\w+\s*=\s*\w+\.filter\s*\{|\w+\.map\s*\{|\w+\.sortedBy`
- **Message**: Computed values from state should use derivedStateOf for optimization.
- **Fix**: Wrap computed value in remember { derivedStateOf { } }.

### Bad
```kotlin
@Composable
fun ListScreen(items: List<Item>, query: String) {
    val filteredItems = items.filter { it.name.contains(query) }
}
```

### Good
```kotlin
@Composable
fun ListScreen(items: List<Item>, query: String) {
    val filteredItems by remember(items, query) {
        derivedStateOf { items.filter { it.name.contains(query) } }
    }
}
```
