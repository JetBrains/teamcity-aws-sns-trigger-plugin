package jetbrains.buildServer.serverSide.identifiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Entity complex identifier.
 * Holds internal and external ids of the entity.
 *
 * <p>
 * The internal identifier is immutable BUT the external one is mutable.
 * </p>
 *
 * @param <INT> type of internal id.
 * @author Leonid Bushuev from JetBrains
 * @since 8.0
 */
public final class EntityId<INT> //implements Comparable<EntityId>, Serializable
{

  /**
   * The internal identifier.
   */
  @NotNull
  private final INT myInternalId;


  /**
   * The external identifier.
   */
  @NotNull
  private volatile String myExternalId;

  /**
   * The identifier of the configuration file.
   *
   * <p>
   * Typically it is a string representation of UUID;
   * however, a user can replace it with their own value.
   * </p>
   *
   * @see UUID
   * @since 9.0
   */
  @NotNull
  private volatile String myConfigId;

  /**
   * Internal id of the project with own versioned settings enabled this entity came from.
   * Set only for deleted entities.
   */
  @Nullable
  private volatile String myOriginProjectIntId;

  /**
   * Delete timestamp or null if the entity is not deleted
   */
  @Nullable
  private volatile Long myDeleteTime;

  /**
   * A trivial constructor.
   *
   * @param internalId         internal identifier.
   * @param externalId         external identifier.
   * @param configId           guid.
   * @param originProjectIntId internal id of the origin project
   * @param deleteTime         delete time
   */
  public EntityId(@NotNull final INT internalId,
                  @NotNull final String externalId,
                  @NotNull final String configId,
                  @Nullable final String originProjectIntId,
                  @Nullable final Long deleteTime) {
    myInternalId = internalId;
    myExternalId = externalId;
    myConfigId = configId;
    myOriginProjectIntId = originProjectIntId;
    myDeleteTime = deleteTime;
  }

  /**
   * Internal id.
   *
   * @return internal id.
   */
  @NotNull
  public INT getInternalId() {
    return myInternalId;
  }

  @NotNull
  public String getConfigId() {
    return myConfigId;
  }

  /**
   * Modifies the holded config identifier.
   *
   * @param newConfigd new identifier.
   */
  void setConfigId(@NotNull final String newConfigd) {
    this.myConfigId = newConfigd;
  }

  /**
   * External id.
   *
   * @return external id.
   */
  @NotNull
  public String getExternalId() {
    return myExternalId;
  }

  /**
   * Modifies the holded external identifier.
   *
   * @param newExternalId new identifier.
   */
  void setExternalId(@NotNull final String newExternalId) {
    this.myExternalId = newExternalId;
  }

  @Nullable
  public String getOriginProjectIntId() {
    return myOriginProjectIntId;
  }

  @Nullable
  public Long getDeleteTime() {
    return myDeleteTime;
  }

  void markRemoved(@Nullable String originProjectIntId, long deleteTime) {
    myOriginProjectIntId = originProjectIntId;
    myDeleteTime = deleteTime;
  }

  void resetRemoved() {
    myOriginProjectIntId = null;
    myDeleteTime = null;
  }

  @Override
  public int hashCode() {
    return (myInternalId.hashCode() * 7) ^ (myExternalId.hashCode() * 3);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final EntityId entityId = (EntityId) o;

    return myExternalId.equals(entityId.myExternalId) && myInternalId.equals(entityId.myInternalId);
  }

  @Override
  public String toString() {
    return myInternalId + ":" + myExternalId;
  }
}
