package com.jiafenbu.androidpaint.command

/**
 * 撤销/重做管理器
 * 使用命令模式管理操作历史
 * @param maxStackSize 最大历史记录数，默认50
 */
class CommandManager(private val maxStackSize: Int = 50) {
    
    /** 撤销栈 */
    private val undoStack = mutableListOf<DrawCommand>()
    
    /** 重做栈 */
    private val redoStack = mutableListOf<DrawCommand>()
    
    /**
     * 执行命令并加入撤销栈
     */
    fun execute(command: DrawCommand) {
        command.execute()
        undoStack.add(command)
        redoStack.clear()
        
        // 限制撤销栈大小
        while (undoStack.size > maxStackSize) {
            undoStack.removeAt(0)
        }
    }
    
    /**
     * 撤销上一个命令
     * @return 是否成功撤销
     */
    fun undo(): Boolean {
        if (!canUndo()) return false
        
        val command = undoStack.removeLast()
        command.undo()
        redoStack.add(command)
        return true
    }
    
    /**
     * 重做上一个撤销的命令
     * @return 是否成功重做
     */
    fun redo(): Boolean {
        if (!canRedo()) return false
        
        val command = redoStack.removeLast()
        command.execute()
        undoStack.add(command)
        return true
    }
    
    /**
     * 是否可以撤销
     */
    fun canUndo(): Boolean = undoStack.isNotEmpty()
    
    /**
     * 是否可以重做
     */
    fun canRedo(): Boolean = redoStack.isNotEmpty()
    
    /**
     * 获取撤销栈大小
     */
    fun getUndoStackSize(): Int = undoStack.size
    
    /**
     * 获取重做栈大小
     */
    fun getRedoStackSize(): Int = redoStack.size
    
    /**
     * 清空所有历史记录
     */
    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
