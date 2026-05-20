package com.rudra.fintechvar.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.rudra.fintechvar.db.modal.SynonymEntity

@Dao
interface SynonymDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(synonyms: List<SynonymEntity>)

    @Query("SELECT * FROM synonyms")
    suspend fun getAllSynonyms(): List<SynonymEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(synonym: SynonymEntity)

    @Query("DELETE FROM synonyms WHERE rawWord = :word")
    suspend fun deleteWord(word: String)
}