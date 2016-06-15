package com.bnsantos.uploader.model;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.sql.SQLException;

/**
 * Created by bruno on 15/06/16.
 */
public class PersistenceManager extends OrmLiteSqliteOpenHelper {
  private static final String DATABASE_NAME = "com.bnsantos.uploader.db";
  private static final int DATABASE_VERSION = 1;

  private Dao mItemDAO = null;

  public PersistenceManager(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
    createTables(connectionSource);
  }

  @Override
  public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
    dropTables();
    // after we drop the old databases, we create the new ones
    onCreate(database, connectionSource);
  }

  private void createTables(ConnectionSource connectionSource) {
    try {
      TableUtils.createTable(connectionSource, Item.class);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  private void dropTables() {
    try {
      TableUtils.dropTable(connectionSource, Item.class, true);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  public Dao getItemDAO() throws SQLException {
    if (mItemDAO == null) {
      mItemDAO = getDao(Item.class);
    }
    return mItemDAO;
  }
}
