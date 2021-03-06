package com.cloudeggtech.granite.cluster.auth;

import javax.annotation.Resource;

import org.bson.Document;
import org.springframework.stereotype.Component;

import com.cloudeggtech.granite.framework.core.auth.Account;
import com.cloudeggtech.granite.framework.core.auth.IAccountManager;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

@Component
public class AccountManager implements IAccountManager {
	@Resource
	private MongoDatabase database;
	
	@Override
	public void add(Account account) {
		Document doc = new Document().
				append("name", account.getName()).
				append("password", account.getPassword());
		getUsersCollection().insertOne(doc);
	}

	@Override
	public void remove(String name) {
		getUsersCollection().deleteOne(Filters.eq("name", name));
	}

	@Override
	public boolean exists(String name) {
		return getUsersCollection().count(Filters.eq("name", name)) == 1;
	}

	@Override
	public Account get(String name) {
		MongoCollection<Document> users = getUsersCollection();
		Document doc = users.find(Filters.eq("name", name)).first();
		if (doc == null)
			return null;
		
		P_Account account = new P_Account();
		account.setId(doc.getObjectId("_id").toHexString());
		account.setName(doc.getString("name"));
		account.setPassword(doc.getString("password"));
		
		return account;
	}
	
	private MongoCollection<Document> getUsersCollection() {
		return database.getCollection("users");
	}

}
