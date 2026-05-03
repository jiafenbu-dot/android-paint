package com.jiafenbu.androidpaint.command

/**
 * 可撤销的命令接口
 * 所有绘图操作都实现此接口以支持撤销/重做
 */
interface DrawCommand {
    /**
     * 执行命令
     */
    fun execute()
    
    /**
     * 撤销命令
     */
    fun undo()
}
