package cn.nekocode.murmur.data.repo

import cn.nekocode.murmur.data.util.CacheUtil

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
object SettingRepo {

    fun loadSelectedMurmursIDs(): List<String>? {
        return CacheUtil.read<List<String>>("selectedMurmursIDs")
    }

    fun saveSelectedMurmursIDs(murmursIDs: List<String>?) {
        CacheUtil.write("selectedMurmursIDs", murmursIDs)
    }

}