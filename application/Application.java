package application;

import model.*;

public class Application {

	public static void main(String[] args) throws Exception
	{
		Repository repo = new Repository();
		repo.start();
	}
}
