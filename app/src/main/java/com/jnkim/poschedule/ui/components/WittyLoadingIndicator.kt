package com.jnkim.poschedule.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Loading indicator with witty rotating messages.
 * Displays humorous status messages that cycle every few seconds
 * to make long LLM processing waits more engaging.
 */
@Composable
fun WittyLoadingIndicator(
    modifier: Modifier = Modifier,
    messages: List<String> = DefaultLoadingMessages
) {
    var currentIndex by remember { mutableIntStateOf(0) }

    // Rotate messages every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            currentIndex = (currentIndex + 1) % messages.size
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()

        Spacer(modifier = Modifier.height(24.dp))

        // Animated text transition with background
        AnimatedContent(
            targetState = messages[currentIndex],
            transitionSpec = {
                fadeIn() + slideInVertically { it / 2 } togetherWith
                        fadeOut() + slideOutVertically { -it / 2 }
            },
            label = "loading_message"
        ) { message ->
            Surface(
                modifier = Modifier.padding(horizontal = 32.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

/**
 * Default witty loading messages in Korean.
 * Mix of technical process descriptions and humorous phrases.
 */
private val DefaultLoadingMessages = listOf(
    "요청을 보냈습니다...",
    "응답을 받았습니다...",
    "상상 중이에요 🤔",
    "허겁지겁 노트 찾고 있어요 📝",
    "깜빡하고 다시 점검하고 있어요 ✓",
    "아이디어를 떠올리고 있어요 💡",
    "열심히 생각하고 있어요 🧠",
    "잠깐, 뭐였더라... 🤔",
    "거의 다 됐어요!",
    "조금만 더 기다려주세요...",
    "최선을 다하고 있어요 💪",
    "곧 완료될 거예요!",
    "정리 중이에요 📋",
    "마무리하고 있어요 ✨",
    "이제 거의 다 왔어요!",
    "한번 더 확인 중이에요 🔍",
    "세부사항을 다듬고 있어요",
    "멋진 일정을 만들고 있어요 ✨",
    "완벽하게 다듬는 중이에요",
    "조금만 더 기다려주세요 🙏"
)
