/**
 * Copyright (c) 2009-2015, Data Geekery GmbH (http://www.datageekery.com)
 * All rights reserved.
 *
 * This work is dual-licensed
 * - under the Apache Software License 2.0 (the "ASL")
 * - under the jOOQ License and Maintenance Agreement (the "jOOQ License")
 * =============================================================================
 * You may choose which license applies to you:
 *
 * - If you're using this work with Open Source databases, you may choose
 *   either ASL or jOOQ License.
 * - If you're using this work with at least one commercial database, you must
 *   choose jOOQ License
 *
 * For more information, please visit http://www.jooq.org/licenses
 *
 * Apache Software License 2.0:
 * -----------------------------------------------------------------------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * jOOQ License and Maintenance Agreement:
 * -----------------------------------------------------------------------------
 * Data Geekery grants the Customer the non-exclusive, timely limited and
 * non-transferable license to install and use the Software under the terms of
 * the jOOQ License and Maintenance Agreement.
 *
 * This library is distributed with a LIMITED WARRANTY. See the jOOQ License
 * and Maintenance Agreement for more details: http://www.jooq.org/licensing
 */
package org.jooq.impl;

import static org.jooq.Clause.CREATE_VIEW;
import static org.jooq.Clause.CREATE_VIEW_AS;
import static org.jooq.Clause.CREATE_VIEW_NAME;
import static org.jooq.SQLDialect.SQLITE;
import static org.jooq.impl.DSL.selectFrom;
import static org.jooq.impl.DSL.table;

import org.jooq.Clause;
import org.jooq.Configuration;
import org.jooq.Context;
import org.jooq.CreateViewAsStep;
import org.jooq.CreateViewFinalStep;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Select;
import org.jooq.Table;

/**
 * @author Lukas Eder
 */
class CreateViewImpl<R extends Record> extends AbstractQuery implements

    // Cascading interface implementations for CREATE VIEW behaviour
    CreateViewAsStep<R>,
    CreateViewFinalStep {


    /**
     * Generated UID
     */
    private static final long     serialVersionUID = 8904572826501186329L;
    private static final Clause[] CLAUSES          = { CREATE_VIEW };

    private final Table<?>        view;
    private final Field<?>[]      fields;
    private Select<?>             select;

    CreateViewImpl(Configuration configuration, Table<?> view, Field<?>[] fields) {
        super(configuration);

        this.view = view;
        this.fields = fields;
    }

    // ------------------------------------------------------------------------
    // XXX: DSL API
    // ------------------------------------------------------------------------

    @Override
    public final CreateViewFinalStep as(Select<? extends R> s) {
        this.select = s;
        return this;
    }

    // ------------------------------------------------------------------------
    // XXX: QueryPart API
    // ------------------------------------------------------------------------

    @Override
    public final void accept(Context<?> ctx) {

        // [#3835] SQLite doesn't like renaming columns at the view level
        boolean rename = fields != null && fields.length > 0;
        boolean renameSupported = ctx.family() != SQLITE;

        ctx.start(CREATE_VIEW_NAME)
           .keyword("create view")
           .sql(" ")
           .visit(view);

        if (rename && renameSupported) {
            boolean qualify = ctx.qualify();

            ctx.sql("(")
               .qualify(false)
               .visit(new QueryPartList<Field<?>>(fields))
               .qualify(qualify)
               .sql(")");
        }

        ctx.end(CREATE_VIEW_NAME)
           .formatSeparator()
           .keyword("as")
           .formatSeparator()
           .start(CREATE_VIEW_AS)
           .visit(
               rename && !renameSupported
             ? selectFrom(table(select).as("t", Utils.fieldNames(fields)))
             : select)
           .end(CREATE_VIEW_AS);
    }

    @Override
    public final Clause[] clauses(Context<?> ctx) {
        return CLAUSES;
    }
}
