import javax.swing.Timer;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class FroggerCtrl
{
	
	PnlFrog frogView;
	FroggerModel model;
	private int nframe = 0;
	private final Random random = new Random();
	private int timerPrize = randTemp();
	private boolean first = true;
	private NPC npc;
	private boolean contact;
	
	private Prize precedente;
	
	public FroggerCtrl(FroggerModel model/*,PnlFrog frogView*/) throws IOException
	{
		this.model = model;
		this.frogView = new PnlFrog(model.entities, this);
		Timer t = new Timer(33, (e) ->
		{
			try
			{
				nextFrame();
				if (first)
				{
					first = false;
					for (int j = 0; j < model.prizes.size(); j++)
					{
						if (model.prizes.get(j).isBonus())
						{
							model.prizes.get(j).stepNext(frogView.destinations);
							for (int i = 0; i < model.prizes.size(); i++)
							{
								if (model.prizes.get(j).hitbox.intersects(model.prizes.get(i).hitbox) && model.prizes.get(j).p.getX() != model.prizes.get(i).p.getX())
									precedente = model.prizes.get(i);
							}
							swapPrize(model.prizes.get(j));
						}
					}
				}
			}
			catch (IOException ex)
			{
				ex.printStackTrace();
			}
		});
		
		t.start();
		
	}
	
	private void nextFrame()
	{
		contact = false;
		npc = model.NPCs.get(0);
		
		if (nframe == 15)
		{
			model.tempo -= 5;
			nframe = 0;
		}
		else
			nframe++;
		
		for (Turtle t : model.turtles)
		{
			if (t.isSub())
			{
				t.immersion();
			}
		}
		
		int size = model.NPCs.size();
		ExecutorService service = Executors.newFixedThreadPool(4);
		
		service.submit(() -> doSomething(0, size / 4));
		service.submit(() -> doSomething(size / 4, size / 2));
		service.submit(() -> doSomething(size / 2, size * 3 / 4));
		service.submit(() -> doSomething(size * 3 / 4, size));
		
		service.shutdown();
		
		try
		{
			service.awaitTermination(3, TimeUnit.MILLISECONDS);
			checkCollision(model.frog);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		
		if (!npc.deathTouch && this.contact)
		{
			model.frog.stepNext(npc.dx);
		}
		
		if (model.frog.getVite() <= 0)
		{
			//todo fare finire il gioco
		}
		
		checkTime(model.frog);
		if (model.frog.p.getY() >= 1200)
			checkPrize(model.frog);
		
		
		updatePrize();
		
		updateSkull();
		
		frogView.setEntities(model.entities);
		frogView.repaint();
		
	}
	
	private void doSomething(int start, int end)
	{
		for (int i = start; i < end; i++)
		{
			model.NPCs.get(i).stepNext();
			if (model.NPCs.get(i).dx > 0)
			{
				if (model.NPCs.get(i).p.getX() - model.NPCs.get(i).getDimx() > 1020)
				{
					model.NPCs.get(i).p.setX(-model.NPCs.get(i).getDimx() - 20);
				}
			}
			else
			{
				if (model.NPCs.get(i).p.getX() + model.NPCs.get(i).getDimx() < -20)
				{
					model.NPCs.get(i).p.setX(1020);
				}
			}
			
			if (model.frog.hitbox.intersects(model.NPCs.get(i).hitbox))
			{
				this.contact = true;
				this.npc = model.NPCs.get(i);
			}
		}
	}
	
	
	private void updateSkull()
	{
		for (Skull s : model.skulls)
		{
			if (s.getTimeToLive() > 0)
			{
				model.entities.add(s);
			}
			else
			{
				model.entities.remove(s);
			}
			s.setTimeToLive(s.getTimeToLive() - 1);
		}
	}
	
	private void checkCollision(Frog frog)
	{
		if ((this.contact && this.npc.deathTouch) || (!this.contact && frog.p.getY() >= 701 && frog.p.getY() <= 1200))
		{
			updateMorte(frog);
		}
	}
	
	
	private void updatePrize()
	{
		timerPrize--;
		if (timerPrize <= 40) //todo definire quanti bonus ci sono
		{
			if (timerPrize % 6 >= 3)
			{
				for (Prize p : model.prizes)
				{
					if (p.isBonus())
					{
						p.setSprite(model.spriteFly);
					}
				}
			}
			else
			{
				for (Prize p : model.prizes)
				{
					if (p.isBonus())
					{
						p.setSprite(model.spriteVoid);
					}
				}
			}
			if (timerPrize <= 0)
			{
				timerPrize = randTemp();
				
				for (int i = 0; i < model.prizes.size(); i++)
				{
					if (model.prizes.get(i).isBonus())
					{
						try
						{
							model.prizes.get(i).stepNext(frogView.destinations);
							swapPrize(model.prizes.get(i));
						}
						catch (IOException e)
						{
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	private void swapPrize(Prize bonus)
	{
		model.prizes.add(precedente);
		model.entities.add(precedente);
		for (int i = 0; i < model.prizes.size(); i++)
		{
			if (bonus.hitbox.intersects(model.prizes.get(i).hitbox) && bonus.p.getX() != model.prizes.get(i).p.getX())
			{
				precedente = model.prizes.get(i);
				model.prizes.remove(precedente);
				model.entities.remove(precedente);
			}
		}
	}
	
	private void updateMorte(Frog frog)
	{
		model.skulls.add(new Skull(frog.p.getX(), frog.p.getY(), 0, model.spriteSkull, 0, 0));
		
		if (model.tempo == 0)
		{
			//	model.sound.soundMorteTempo();
		}
		else
		{
			if (frog.p.getY() > 700 && frog.p.getY() < 1200)
			{
				Sound.soundMorteAcqua();
			}
			else
			{
				Sound.soundMorteAuto();
			}
		}
		try
		{
			frog.morte();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		resetTempo();
	}
	
	private void checkTime(Frog frog)
	{
		if (model.tempo <= 0)
			updateMorte(frog);
		
	}
	
	private void checkPrize(Frog frog)
	{
		
		boolean save = false;
		
		for (Prize p : model.prizes)
		{
			if (frog.hitbox.intersects(p.hitbox))
			{
				
				updatePoint(frog, p.getPoint());
				
				for (int i = 0; i < frogView.destinations.size(); i++)
				{
					if (distance(frog.p, frogView.destinations.get(i)) <= 100)
						frogView.destinations.remove(i);
				}
				
				if (p.isBonus())
				{
					resetBonus(p);
				}
				else
				{
					p.setSprite(model.spriteFrogLily);
					p.setHitbox(null);
					model.prizes.remove(p);
				}
				
				try
				{
					frog.resetPosition();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
				resetTempo();
				
				save = true;
				
				break;
			}
		}
		
		if (!save)
			updateMorte(frog);
	}
	
	private void resetBonus(Prize bonus)
	{
		try
		{
			bonus.stepNext(frogView.destinations);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		timerPrize = randTemp();
		model.entities.add(precedente);
		precedente.setSprite(model.spriteFrogLily);
		precedente.setHitbox(null);
		
		for (int i = 0; i < model.prizes.size(); i++)
		{
			if (model.prizes.size() == 1)
			{
				model.prizes.add(precedente);
				model.entities.add(precedente);
				model.prizes.remove(bonus);
				model.entities.remove(bonus);
				//todo fermare il gioco perché si ha vinto
			}
			else if (bonus.hitbox.intersects(model.prizes.get(i).hitbox) && bonus.p.getX() != model.prizes.get(i).p.getX())
			{
				precedente = model.prizes.get(i);
				model.prizes.remove(precedente);
				model.entities.remove(precedente);
			}
		}
	}
	
	
	private int randTemp()
	{
		return random.nextInt(150) + 100;
	}
	
	
	private double distance(Entity.Position p1, Entity.Position p2)
	{
		return Math.sqrt(Math.pow((p1.getX() - p2.getX()), 2) + Math.pow((p1.getY() - p2.getY()), 2));
	}
	
	
	/**
	 * Aggiorno il punteggio della rana in base a quello che ha fatto
	 *
	 * @param frog,  La rana da aggiornare
	 * @param point, I punti base dello sprite raggiunto
	 */
	private void updatePoint(Frog frog, int point)
	{
		frog.setPoint(frog.getPoint() + point + 100 * frog.getPoint() + 5 * model.tempo);
		Sound.soundPoint();
	}
	
	
	/**
	 * Resetta il tempo ogni volta che viene chiamato
	 */
	private void resetTempo()
	{
		model.tempo = 500; //todo mettere costanti ovunque
	}
	
}
