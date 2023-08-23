package com.group.libraryapp.domain.user;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = 2002150608L;

    public static final QUser user = new QUser("user");

    public final NumberPath<Integer> age = createNumber("age", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final ListPath<com.group.libraryapp.domain.user.loadhistory.UserLoanHistory, com.group.libraryapp.domain.user.loadhistory.QUserLoanHistory> userLoanHistories = this.<com.group.libraryapp.domain.user.loadhistory.UserLoanHistory, com.group.libraryapp.domain.user.loadhistory.QUserLoanHistory>createList("userLoanHistories", com.group.libraryapp.domain.user.loadhistory.UserLoanHistory.class, com.group.libraryapp.domain.user.loadhistory.QUserLoanHistory.class, PathInits.DIRECT2);

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}

