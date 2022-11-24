package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Task;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.utils.Utils;

import java.sql.Time;

/*
 Classe pour tester updateProgress et progressbar
 */
public class WaitTaskService extends EsupSgcTaskService<Void> {

	// permet d'étalonner la progressbar en fonction de la dernière exécution de cette même tâche.
	// on utiliser lastRunTime-1000 au niveau de la progressbar pour afficher un temps donné la barre complète
	// (alors que la tâche n'est en fait pas complètement finie [1sec]
	static long lastRunTime = 5000;

	public WaitTaskService(TaskParamBean taskParamBean) {
		super(taskParamBean);
	}

	protected Task<Void> createTask() {
		Task<Void> waitTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				long timeToWait = 10000;
				long start = System.currentTimeMillis();
				long t;
				while ((t = System.currentTimeMillis() - start) < timeToWait) {
					updateTitle("...");
					updateProgress(t, Math.max(lastRunTime-1000, t));
					Utils.sleep(1000);
				}
				lastRunTime = t;
				return null;
			}
		};
		return waitTask;
	}

	@Override
	public EsupSgcTaskService getNextWhenSuccess() {
		return new WaitTaskService(taskParamBean);
	}
}
