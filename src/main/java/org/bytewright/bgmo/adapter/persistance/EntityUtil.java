package org.bytewright.bgmo.adapter.persistance;

import org.hibernate.proxy.HibernateProxy;

public final class EntityUtil {

  private EntityUtil() {}

  public static Class<?> getClassWithoutProxy(Object o) {
    if (o instanceof HibernateProxy hibernateProxy) {
      return hibernateProxy.getHibernateLazyInitializer().getPersistentClass();
    }
    return o.getClass();
  }
}
