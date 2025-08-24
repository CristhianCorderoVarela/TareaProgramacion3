package cr.ac.una.tareaprogramacion3.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JPAUtil {
    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("ProyectoPU");

    public static EntityManager getEM() {
        return emf.createEntityManager();
    }
}
