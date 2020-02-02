@file:Suppress("unused")

package dev.alpas.ozone

import com.github.javafaker.Faker
import me.liuwj.ktorm.dsl.insertAndGenerateKey
import me.liuwj.ktorm.entity.Entity
import me.liuwj.ktorm.entity.findById
import me.liuwj.ktorm.schema.BaseTable

val faker by lazy(LazyThreadSafetyMode.NONE) { Faker() }

/**
 * A factory for creating one or many instances of an Entity class.
 */
interface EntityFactory<E : Entity<*>> {

    /**
     * The entity's table
     */
    val table: BaseTable<E>

    /**
     * Build and persist an entity to a database. Before persisting, its property will be overriden by the
     * corresponding value in the attributes map if the key exists. If the attributes map contains a key
     * that doesn't match with any of the *declared* column in the entity's table, it will be ignored.
     *
     * After persisting the entity in the database, this method retrieves a fresh copy of the entity from
     * the database. This allows us have all its references available, for an example. This means calling
     * this method runs two database queries - one for saving and one for retrieving a fresh copy.
     *
     * @param attrs A map of attributes for overriding entity's properties. Non-existent keys will be ignored.
     * @return A fresh entity from the database.
     *
     */
    fun create(attrs: Map<String, Any?> = emptyMap()): E {
        return saveAndRefresh(build(attrs))
    }

    /**
     * Persist the given entity in the database and return its primary key. The entity's properties
     * are inspected first to make sure that the corresponding columns exist in the entity'stable.
     * If the table doesn't have those columns, those properties are ignored and not persisted.
     *
     * @param entity An entity to persist in the database.
     * @return The primary key of the entity after saving it in the database.
     */
    fun save(entity: E): Any {
        return table.insertAndGenerateKey { builder ->
            entity.properties.forEach { (key, value) ->
                table.columns.find { it.name == key }?.let {
                    builder[key] to value
                }
            }
        }
    }

    /**
     * Persist the given entity in the database and return a fresh copy of it. The entity's properties
     * are inspected first to make sure that the corresponding columns exist in the entity'stable.
     * If the table doesn't have those columns, those properties are ignored and not persisted.
     *
     * @param entity An entity to persist in the database.
     * @return The fresh copy of the entity from the database.
     */
    fun saveAndRefresh(entity: E): E {
        return save(entity).let {
            table.findById(it)!!
        }
    }

    /**
     * Build an entity replacing its properties with the values from the given attributes map. All
     * the extra attributes from the map will be available as the entity's properties as well.
     *
     * @param attrs A map of attributes for overriding entity's properties.
     */

    fun build(attrs: Map<String, Any?> = emptyMap()): E {
        return entity().also {
            attrs.forEach { (key, value) -> it[key] = value }
        }
    }

    /**
     * The actual entity that this factory is responsible for building. An implementor of this interface
     * should not persist it in the database but simply just return an instance of it.
     */
    fun entity(): E
}

/**
 * A convenience proxy method for creating one instance of an entity using the given factory.
 * This method calls `create()` method of the factory, which means it will be persisted.
 *
 * @param factory The factory responsible for creating the actual instance.
 * @param attrs A map of attributes for overriding entity's properties. Non-existent keys will be ignored.
 * @return A fresh entity from the database.
 */
fun <E, EF : EntityFactory<E>> from(factory: EF, attrs: Map<String, Any?> = emptyMap()): E {
    return factory.create(attrs)
}

/**
 * A convenience proxy method for creating one instance of an entity using the given factory.
 * This method calls `create()` method of the factory, which means it will be persisted.
 *
 * @param factory The factory responsible for creating the actual instance.
 * @param attr A pair of attribute for overriding entity's properties. Non-existent keys will be ignored.
 * @param attrs Pairs of attributes for overriding entity's properties. Non-existent keys will be ignored.
 * @return A fresh entity from the database.
 */
fun <E, EF : EntityFactory<E>> from(factory: EF, attr: Pair<String, Any?>, vararg attrs: Pair<String, Any?>): E {
    return from(factory, mapOf(attr, *attrs))
}

/**
 * A convenience proxy method for creating one or more instances of an entity using the given factory.
 * This method calls `create()` method of the factory, which means it will be persisted.
 *
 * @param factory The factory responsible for creating the actual instance.
 * @param count The number of entity instances to create.
 * @param attrs A map of attributes for overriding entity's properties. Non-existent keys will be ignored.
 * @return A list of fresh entities from the database.
 */
fun <E, EF : EntityFactory<E>> from(
    factory: EF,
    count: Int = 1,
    attrs: Map<String, Any?> = emptyMap()
): List<E> {
    return (1..count).map { from(factory, attrs) }
}

/**
 * A convenience proxy method for creating one or more instances of an entity using the given factory.
 * This method calls `create()` method of the factory, which means it will be persisted.
 *
 * @param factory The factory responsible for creating the actual instance.
 * @param count The number of entity instances to create.
 * @param attr A pair of attribute for overriding entity's properties. Non-existent keys will be ignored.
 * @param attrs Pairs of attributes for overriding entity's properties. Non-existent keys will be ignored.
 * @return A fresh entity from the database.
 */
fun <E, EF : EntityFactory<E>> from(
    factory: EF,
    count: Int = 1,
    attr: Pair<String, Any?>,
    vararg attrs: Pair<String, Any?>
): List<E> {
    return from(factory, count, mapOf(attr, *attrs))
}