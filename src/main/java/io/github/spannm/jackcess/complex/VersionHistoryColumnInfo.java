package io.github.spannm.jackcess.complex;

/**
 * Complex column info for a column which tracking the version history of an "append only" memo column.
 * <p>
 * Note, the strongly typed update/delete methods are <i>not</i> supported for version history columns (the data is
 * supposed to be immutable). That said, the "raw" update/delete methods are supported for those that <i>really</i> want
 * to muck with the version history data.
 */
public interface VersionHistoryColumnInfo extends ComplexColumnInfo<Version> {

}
