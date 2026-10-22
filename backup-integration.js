/*
 * Sampan Store: Native Android backup integration
 * Add this script near the end of your index.html, just before </body>.
 * The existing app can keep its own localStorage keys; this backs up all of them.
 */
(function () {
  'use strict';

  function snapshotLocalStorage() {
    var snapshot = {};
    for (var i = 0; i < localStorage.length; i++) {
      var key = localStorage.key(i);
      if (key !== null) snapshot[key] = localStorage.getItem(key);
    }
    return snapshot;
  }

  var backupTimer = null;
  function scheduleAutoBackup() {
    if (!window.SampanBackup || typeof window.SampanBackup.saveAutoBackup !== 'function') return;
    clearTimeout(backupTimer);
    backupTimer = setTimeout(function () {
      try { window.SampanBackup.saveAutoBackup(JSON.stringify(snapshotLocalStorage())); } catch (e) {}
    }, 500);
  }

  // Back up the current data on launch, then check if native private backup can recover empty storage.
  window.sampanRestoreFromNativeBackup = function () {
    try {
      if (localStorage.length > 0) {
        scheduleAutoBackup();
        return;
      }
      if (!window.SampanBackup || !window.SampanBackup.getAutoBackup) return;
      var raw = window.SampanBackup.getAutoBackup();
      if (!raw) return;
      var backup = JSON.parse(raw);
      if (!backup || typeof backup !== 'object' || Array.isArray(backup)) return;
      Object.keys(backup).forEach(function (key) {
        localStorage.setItem(key, String(backup[key] == null ? '' : backup[key]));
      });
      if (window.SampanBackup.showMessage) window.SampanBackup.showMessage('আগের অটো ব্যাকআপ থেকে হিসাব ফিরিয়ে আনা হয়েছে।');
      // Reload so the original app reads its restored values.
      setTimeout(function () { window.location.reload(); }, 250);
    } catch (e) {
      if (window.SampanBackup && window.SampanBackup.showMessage) window.SampanBackup.showMessage('অটো ব্যাকআপ পড়া যায়নি।');
    }
  };

  window.sampanExportBackup = function () {
    try {
      if (!window.SampanBackup || !window.SampanBackup.exportBackup) {
        alert('এই সুবিধাটি Android অ্যাপে ব্যবহার করুন।');
        return;
      }
      window.SampanBackup.exportBackup(JSON.stringify(snapshotLocalStorage()));
    } catch (e) { alert('ব্যাকআপ তৈরি করা যায়নি।'); }
  };

  window.sampanChooseBackupToRestore = function () {
    if (window.SampanBackup && window.SampanBackup.importBackup) window.SampanBackup.importBackup();
    else alert('এই সুবিধাটি Android অ্যাপে ব্যবহার করুন।');
  };

  window.sampanImportBackupFromNative = function (jsonString) {
    try {
      var backup = JSON.parse(jsonString);
      if (!backup || typeof backup !== 'object' || Array.isArray(backup)) throw new Error('Invalid backup');
      // Replace all keys to avoid mixing the imported backup with current data.
      localStorage.clear();
      Object.keys(backup).forEach(function (key) {
        localStorage.setItem(key, String(backup[key] == null ? '' : backup[key]));
      });
      if (window.SampanBackup && window.SampanBackup.saveAutoBackup) {
        window.SampanBackup.saveAutoBackup(JSON.stringify(snapshotLocalStorage()));
      }
      alert('ব্যাকআপ রিস্টোর সম্পন্ন হয়েছে। অ্যাপ রিফ্রেশ হচ্ছে।');
      window.location.reload();
    } catch (e) {
      alert('ব্যাকআপ ফাইলটি পড়া যায়নি।');
    }
  };

  // Back up whenever this app writes to localStorage.
  var originalSetItem = Storage.prototype.setItem;
  Storage.prototype.setItem = function (key, value) {
    originalSetItem.call(this, key, value);
    if (this === localStorage) scheduleAutoBackup();
  };
  var originalRemoveItem = Storage.prototype.removeItem;
  Storage.prototype.removeItem = function (key) {
    originalRemoveItem.call(this, key);
    if (this === localStorage) scheduleAutoBackup();
  };
  var originalClear = Storage.prototype.clear;
  Storage.prototype.clear = function () {
    originalClear.call(this);
    if (this === localStorage) scheduleAutoBackup();
  };

  // Buttons can call these two functions from your app's settings/backup area:
  // onclick="sampanExportBackup()" and onclick="sampanChooseBackupToRestore()"
  window.addEventListener('pagehide', scheduleAutoBackup);
})();
