package com.example.albuddy.data.repository

import com.example.albuddy.data.local.CommandDao
import com.example.albuddy.data.local.VoskWordDao
import com.example.albuddy.data.model.VoskWord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskDictionaryRepository @Inject constructor(
    private val commandDao: CommandDao,
    private val voskWordDao: VoskWordDao
) {
    fun getGrammarJson(): Flow<String> {
        return combine(
            commandDao.getAllCommands(),
            voskWordDao.getAllWords()
        ) { commands, voskWords ->
            val wordsSet = mutableSetOf<String>()
            
            // Add words from commands
            commands.forEach { command ->
                wordsSet.add(command.triggerPhrase.lowercase())
            }
            
            // Add custom vosk words
            voskWords.forEach { voskWord ->
                wordsSet.add(voskWord.word.lowercase())
            }
            
            // Add safety tag
            wordsSet.add("[unk]")
            
            val jsonArray = JSONArray()
            wordsSet.forEach { word ->
                jsonArray.put(word)
            }
            
            jsonArray.toString()
        }
    }

    suspend fun addWord(word: String) {
        if (word.isNotBlank()) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                voskWordDao.insertWord(VoskWord(word = word.trim()))
            }
        }
    }

    suspend fun removeWord(voskWord: VoskWord) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            voskWordDao.deleteWord(voskWord)
        }
    }
    
    fun getAllCustomWords(): Flow<List<VoskWord>> {
        return voskWordDao.getAllWords()
    }
}
