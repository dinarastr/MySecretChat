package ru.yandexpraktikum.blechat.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.yandexpraktikum.blechat.domain.model.Message

@Composable
fun MessageItem(
    message: Message,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (message.isFromLocalUser) 
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = if (message.isFromLocalUser)
                Alignment.End
            else
                Alignment.Start
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}