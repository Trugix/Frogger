import javax.swing.Timer;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.Serializable;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class FroggerCtrl implements KeyListener, MouseListener, Serializable
{
	
	PnlFrog frogView;
	public FroggerModel modelToDraw;
	private int nFrame=0;
	private final Random random = new Random();
	private int timerPrize = randTemp();
	private boolean first = true;
	private NPC npcContact;
	private boolean contact;
	
	private Prize precedente;

	//private Client client = new Client();

	//Server server;

	private Timer t= new Timer(33, (e) ->
	{

		try {
			nextFrame();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		if (this.first)
		{
			initialization();
		}
	});
	
	public FroggerCtrl(FroggerModel model) throws IOException
	{
		this.modelToDraw = model;
		this.frogView = new PnlFrog(this);
		frogView.addKeyListener(this);
		frogView.addMouseListener(this);

	//	this.server = new Server(this);

		if(PnlFrog.state == PnlFrog.STATE.GAME)
			t.start();
	}

	public void start ()
	{
		t.start();
	}
	
	private void initialization()
	{
		this.first = false;
		for (int j = 0; j < modelToDraw.prizes.size(); j++)
		{
			Prize prize1 = modelToDraw.prizes.get(j);
			if (prize1.isBonus())
			{
				prize1.stepNext(frogView.destinations);

				for (int i = 0; i < modelToDraw.prizes.size(); i++)
				{
					Prize prize2 = modelToDraw.prizes.get(i);
					if (prize1.hitbox.intersects(prize2.hitbox) && prize1.p.getX() != prize2.p.getX())
						precedente = prize2;
				}
				swapPrize(prize1);
			}
		}
	}
	
	
	private void nextFrame() throws IOException {
		modelToDraw.tempo--;

		contact = false;

		npcContact = modelToDraw.NPCs.get(0);

		if(modelToDraw.frog.isMoving())
		{
			nFrame++;
			modelToDraw.frog.nextSlide();
			if (nFrame>=5) {
				nFrame = 0;
				modelToDraw.frog.setMoving(false);
			}
		}else {
			modelToDraw.frog.rotate(modelToDraw.frog.getDirection());
		}

		for (Turtle t : modelToDraw.turtles)
		{
			t.immersion();
		}



		int size = modelToDraw.NPCs.size();
		ExecutorService service = Executors.newFixedThreadPool(4);
		
		service.submit(() -> moveNpc(0, size / 4));
		service.submit(() -> moveNpc(size / 4, size / 2));
		service.submit(() -> moveNpc(size / 2, size * 3 / 4));
		service.submit(() -> moveNpc(size * 3 / 4, size));
		
		service.shutdown();

		try
		{
			service.awaitTermination(3, TimeUnit.MILLISECONDS);
			checkCollision(modelToDraw.frog);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		if (!npcContact.deathTouch && this.contact)
		{
			modelToDraw.frog.stepNext(npcContact.dx);
		}
		
		if (modelToDraw.frog.getVite() <= 0)
		{
			frogView.state = PnlFrog.STATE.GAME_OVER;
			t.stop();
		}
		
		checkTime(modelToDraw.frog);
		if (modelToDraw.frog.p.getY() >= 1200)
			checkPrize(modelToDraw.frog);
		

		updatePrize();
		
		updateSkull();
		
		frogView.setEntities(modelToDraw.entities);
		frogView.repaint();
		//server.sender();
		
	}
	
	private void moveNpc(int start, int end)
	{
		for (int i = start; i < end; i++)
		{
			NPC npc = modelToDraw.NPCs.get(i);
			npc.stepNext();
			if (npc.dx > 0)
			{
				if (npc.p.getX() - npc.getDimx() > 1020)
				{
					npc.p.setX(-npc.getDimx() - 20);
				}
			}
			else
			{
				if (npc.p.getX() + npc.getDimx() < -20)
				{
					npc.p.setX(1020);
				}
			}
			
			if (modelToDraw.frog.hitbox.intersects(npc.hitbox))
			{
				this.contact = true;
				this.npcContact = npc;
			}
		}
	}
	
	
	private void updateSkull()
	{
		for (Skull s : modelToDraw.skulls)
		{
			if (s.getTimeToLive() > 0)
			{
				modelToDraw.entities.add(s);
			}
			else
			{
				modelToDraw.entities.remove(s);
			}
			s.setTimeToLive(s.getTimeToLive() - 1);
		}
	}
	
	private void checkCollision(Frog frog)
	{
		if ((this.contact && this.npcContact.deathTouch) || (!this.contact && frog.p.getY() >= 701 && frog.p.getY() <= 1200))
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
				for (Prize p : modelToDraw.prizes)
				{
					if (p.isBonus())
					{
						p.setSprite(modelToDraw.spriteFly);
					}
				}
			}
			else
			{
				for (Prize p : modelToDraw.prizes)
				{
					if (p.isBonus())
					{
						p.setSprite(modelToDraw.spriteVoid);
					}
				}
			}
			if (timerPrize <= 0)
			{
				timerPrize = randTemp();
				
				for (int i = 0; i < modelToDraw.prizes.size(); i++)
				{
					if (modelToDraw.prizes.get(i).isBonus())
					{
						modelToDraw.prizes.get(i).stepNext(frogView.destinations);
						swapPrize(modelToDraw.prizes.get(i));
					}
				}
			}
		}
	}
	
	private void swapPrize(Prize bonus)
	{
		modelToDraw.prizes.add(precedente);
		modelToDraw.entities.add(precedente);
		for (int i = 0; i < modelToDraw.prizes.size(); i++)
		{
			if (bonus.hitbox.intersects(modelToDraw.prizes.get(i).hitbox) && bonus.p.getX() != modelToDraw.prizes.get(i).p.getX())
			{
				precedente = modelToDraw.prizes.get(i);
				modelToDraw.prizes.remove(precedente);
				modelToDraw.entities.remove(precedente);
			}
		}
	}
	
	private void updateMorte(Frog frog)
	{
		modelToDraw.skulls.add(new Skull(frog.p.getX(), frog.p.getY(), 0, modelToDraw.spriteSkull, 0, 0));
		if (frog.p.getY() > 700 && frog.p.getY() < 1200)
		{
			Sound.soundMorteAcqua();
		}
		else
		{
			Sound.soundMorteAuto();
		}
		try
		{
			nFrame=0;
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
		if (modelToDraw.tempo == 110)
		{
			Sound.soundTicToc();
		}
		if (modelToDraw.tempo <= 0)
			updateMorte(frog);
		
	}
	
	private void checkPrize(Frog frog)
	{
		
		boolean save = false;
		
		for (Prize p : modelToDraw.prizes)
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
					p.setSprite(modelToDraw.spriteFrogLily);
					p.setHitbox(null);
					modelToDraw.prizes.remove(p);
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

		bonus.stepNext(frogView.destinations);

		timerPrize = randTemp();
		modelToDraw.entities.add(precedente);
		precedente.setSprite(modelToDraw.spriteFrogLily);
		precedente.setHitbox(null);
		
		for (int i = 0; i < modelToDraw.prizes.size(); i++)
		{
			if (modelToDraw.prizes.size() == 1)
			{
				modelToDraw.prizes.add(precedente);
				modelToDraw.entities.add(precedente);
				modelToDraw.prizes.remove(bonus);
				modelToDraw.entities.remove(bonus);
				//todo fermare il gioco perché si ha vinto
			}
			else if (bonus.hitbox.intersects(modelToDraw.prizes.get(i).hitbox) && bonus.p.getX() != modelToDraw.prizes.get(i).p.getX())
			{
				precedente = modelToDraw.prizes.get(i);
				modelToDraw.prizes.remove(precedente);
				modelToDraw.entities.remove(precedente);
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
		frog.setPoint(frog.getPoint() + point + 100 * frog.getVite() + 5 * modelToDraw.tempo);
		Sound.soundPoint();
	}
	
	
	/**
	 * Resetta il tempo ogni volta che viene chiamato
	 */
	private void resetTempo()
	{
		modelToDraw.tempo = 500; //todo mettere costanti ovunque
	}
	
	private void aggiornaPanelClient()
	{
		//client.setPanel(frogView);
	}

	@Override
	public void keyTyped(KeyEvent e) {

	}

	@Override
	public void keyPressed(KeyEvent e) {
		try
		{
			if (!modelToDraw.frog.isMoving())
				modelToDraw.moveFrog(e.getKeyCode());
		}
		catch (IOException ex)
		{
			ex.printStackTrace();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	@Override
	public void mouseClicked(MouseEvent e) {

	}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if (frogView.state == PnlFrog.STATE.MENU)
			if(e.getX() >= 169 && e.getX() <= 498 &&  e.getY() >= 224 && e.getY() <= 320)
			{
				frogView.state = PnlFrog.STATE.GAME;
				frogView.paintComponent(frogView.g2);
				start();
			}


		System.out.println(" "+e.getX()+" "+ e.getY());

		System.out.println(PnlFrog.state);


	}

	@Override
	public void mouseReleased(MouseEvent e) {

	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}
}
