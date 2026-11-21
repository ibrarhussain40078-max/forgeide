package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.example.data.model.CodeFile
import com.example.data.model.FileLanguage
import com.example.data.model.IdeMode
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "ide_projects")
data class ProjectEntity(
    @PrimaryKey val modeKey: String,
    val projectName: String,
    val activeFileName: String,
    val updatedAt: Long
)

@Entity(
    tableName = "ide_files",
    primaryKeys = ["modeKey", "fileName"]
)
data class ProjectFileEntity(
    val modeKey: String,
    val fileName: String,
    val content: String,
    val sortOrder: Int
)

@Dao
interface IdeDao {
    @Query("SELECT * FROM ide_projects WHERE modeKey = :modeKey LIMIT 1")
    suspend fun getProject(modeKey: String): ProjectEntity?

    @Query("SELECT * FROM ide_files WHERE modeKey = :modeKey ORDER BY sortOrder ASC")
    suspend fun getFiles(modeKey: String): List<ProjectFileEntity>

    @Query("SELECT * FROM ide_projects")
    fun observeAllProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProject(project: ProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<ProjectFileEntity>)

    @Query("DELETE FROM ide_files WHERE modeKey = :modeKey")
    suspend fun deleteFilesForMode(modeKey: String)

    @Transaction
    suspend fun saveWorkspace(
        modeKey: String,
        projectName: String,
        activeFileName: String,
        files: List<CodeFile>
    ) {
        upsertProject(
            ProjectEntity(
                modeKey = modeKey,
                projectName = projectName,
                activeFileName = activeFileName,
                updatedAt = System.currentTimeMillis()
            )
        )
        deleteFilesForMode(modeKey)
        insertFiles(
            files.mapIndexed { index, file ->
                ProjectFileEntity(
                    modeKey = modeKey,
                    fileName = file.name,
                    content = file.content,
                    sortOrder = index
                )
            }
        )
    }
}

@Database(
    entities = [ProjectEntity::class, ProjectFileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class IdeDatabase : RoomDatabase() {
    abstract fun ideDao(): IdeDao

    companion object {
        @Volatile
        private var INSTANCE: IdeDatabase? = null

        fun getInstance(context: Context): IdeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    IdeDatabase::class.java,
                    "forge_ide_database"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

data class LoadedWorkspace(
    val projectName: String,
    val activeFileName: String,
    val files: List<CodeFile>,
    val updatedAt: Long
)

class IdeRepository(private val dao: IdeDao) {
    val projectsFlow: Flow<List<ProjectEntity>> = dao.observeAllProjects()

    suspend fun loadWorkspace(mode: IdeMode): LoadedWorkspace? {
        val project = dao.getProject(mode.name) ?: return null
        val fileEntities = dao.getFiles(mode.name)
        if (fileEntities.isEmpty()) return null
        val codeFiles = fileEntities.map {
            CodeFile(
                name = it.fileName,
                content = it.content,
                language = FileLanguage.fromFileName(it.fileName)
            )
        }
        return LoadedWorkspace(
            projectName = project.projectName,
            activeFileName = project.activeFileName,
            files = codeFiles,
            updatedAt = project.updatedAt
        )
    }

    suspend fun saveWorkspace(
        mode: IdeMode,
        projectName: String,
        activeFileName: String,
        files: List<CodeFile>
    ) {
        dao.saveWorkspace(
            modeKey = mode.name,
            projectName = projectName,
            activeFileName = activeFileName,
            files = files
        )
    }
}
