package com.example.myproject.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.example.myproject.generated.resources.Res
import com.example.myproject.generated.resources.cd_clear_search
import com.example.myproject.generated.resources.cd_search
import com.example.myproject.ui.theme.AppTheme
import com.example.myproject.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * Styled search text field with leading search icon and trailing clear button.
 *
 * Handles keyboard IME action and clears the field when the X button is tapped.
 * Follows Material 3 styling using the app's shape and colour tokens.
 *
 * Usage:
 * ```kotlin
 * var query by remember { mutableStateOf("") }
 *
 * SearchBar(
 *     query = query,
 *     onQueryChange = { query = it },
 *     placeholder = "Search items...",
 *     modifier = Modifier.fillMaxWidth()
 * )
 * ```
 *
 * To react to the search (e.g. trigger network request on keyboard "Search"):
 * ```kotlin
 * SearchBar(
 *     query = query,
 *     onQueryChange = { query = it },
 *     placeholder = "Search users...",
 *     onSearch = { performSearch(query) }
 * )
 * ```
 *
 * @param query         Current search text
 * @param onQueryChange Called whenever the text changes
 * @param placeholder   Hint text shown when the field is empty
 * @param modifier      Applied to the [OutlinedTextField]
 * @param onSearch      Optional callback for the keyboard "Search" / "Done" action
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onSearch: (() -> Unit)? = null,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.paddingMedium,
                vertical = Dimens.spacingCompact,
            ),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(Res.string.cd_search),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onQueryChange("")
                        keyboardController?.hide()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(Res.string.cd_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                onSearch?.invoke()
                keyboardController?.hide()
            },
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Preview
@Composable
@Suppress("UnusedPrivateMember")
private fun SearchBarPreview() {
    AppTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = Dimens.paddingSmall),
        ) {
            SearchBar(
                query = "",
                onQueryChange = {},
                placeholder = "Search items...",
            )
            SearchBar(
                query = "hello world",
                onQueryChange = {},
                placeholder = "Search items...",
            )
        }
    }
}
